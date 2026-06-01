package upc.com.pe.backendplannia.project.application.internal.commandservices;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Task;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateTaskCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.MarkTaskAsAssignedCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.SyncTaskAssignmentStatusCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.UpdateTaskCommand;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;
import upc.com.pe.backendplannia.project.domain.services.AssignmentActivityPort;
import upc.com.pe.backendplannia.project.domain.services.TaskCommandService;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories.CategoryRepository;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories.TaskRepository;
import upc.com.pe.backendplannia.shared.domain.model.events.TaskMarkedAsDoneEvent;

import java.util.Optional;

@Service
public class TaskCommandServiceImpl implements TaskCommandService {
    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final AssignmentActivityPort assignmentActivityPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    public TaskCommandServiceImpl(
            TaskRepository taskRepository,
            CategoryRepository categoryRepository,
            AssignmentActivityPort assignmentActivityPort,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
        this.assignmentActivityPort = assignmentActivityPort;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    @Transactional
    public Optional<Task> handle(CreateTaskCommand command) {
        var category = categoryRepository.findById(command.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        var task = new Task(command, category);
        category.addTask(task);
        return Optional.of(taskRepository.save(task));
    }

    @Override
    @Transactional
    public Optional<Task> handle(MarkTaskAsAssignedCommand command) {
        var task = taskRepository.findById(command.taskId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        task.markAssigned();
        return Optional.of(taskRepository.save(task));
    }

    @Override
    @Transactional
    public void handle(SyncTaskAssignmentStatusCommand command) {
        for (Long taskId : command.taskIds()) {
            taskRepository.findById(taskId).ifPresent(task -> {
                if (!assignmentActivityPort.isLatestAssignmentUserActive(taskId)) {
                    task.markUnassigned();
                    taskRepository.save(task);
                }
            });
        }
    }

    @Override
    @Transactional
    public Optional<Task> handle(UpdateTaskCommand command) {
        var task = taskRepository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        var previousStatus = task.getStatus();
        task.update(command);
        var savedTask = taskRepository.save(task);

        var category = task.getCategory();
        var categoryTasks = taskRepository.findByCategory_Id(category.getId());
        category.syncStatusFromTasks(categoryTasks);
        categoryRepository.save(category);

        if (savedTask.getStatus() == Status.DONE && previousStatus != Status.DONE) {
            assignmentActivityPort.findLatestAssignmentUserId(savedTask.getId())
                    .ifPresent(userId -> applicationEventPublisher.publishEvent(
                            new TaskMarkedAsDoneEvent(savedTask.getId(), userId)
                    ));
        }

        return Optional.of(savedTask);
    }
}
