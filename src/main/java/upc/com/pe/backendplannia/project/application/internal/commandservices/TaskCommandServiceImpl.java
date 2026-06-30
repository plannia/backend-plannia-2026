package upc.com.pe.backendplannia.project.application.internal.commandservices;

import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskCommandServiceImpl.class);

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
        LOGGER.info(
                "Task update requested: taskId={}, requestedStatus={}, limitDate={}",
                command.id(),
                command.status(),
                command.limitDate()
        );

        var task = taskRepository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        var previousStatus = task.getStatus();
        LOGGER.info(
                "Task loaded before update: taskId={}, previousStatus={}, assigned={}, categoryId={}",
                task.getId(),
                previousStatus,
                task.isAssigned(),
                task.getCategory().getId()
        );

        task.update(command);
        var savedTask = taskRepository.save(task);
        LOGGER.info(
                "Task saved after update: taskId={}, previousStatus={}, newStatus={}, startTime={}, endTime={}",
                savedTask.getId(),
                previousStatus,
                savedTask.getStatus(),
                savedTask.getStartTime(),
                savedTask.getEndTime()
        );

        var category = task.getCategory();
        var categoryTasks = taskRepository.findByCategory_Id(category.getId());
        category.syncStatusFromTasks(categoryTasks);
        categoryRepository.save(category);
        LOGGER.info(
                "Category status synced after task update: taskId={}, categoryId={}, categoryStatus={}, taskCount={}",
                savedTask.getId(),
                category.getId(),
                category.getStatus(),
                categoryTasks.size()
        );

        if (savedTask.getStatus() == Status.DONE && previousStatus != Status.DONE) {
            LOGGER.info(
                    "Task transitioned to DONE; resolving latest assignment before publishing event: taskId={}",
                    savedTask.getId()
            );
            try {
                var latestAssignmentUserId = assignmentActivityPort.findLatestAssignmentUserId(savedTask.getId());
                if (latestAssignmentUserId.isEmpty()) {
                    LOGGER.warn(
                            "Task DONE update found no latest assignment user; no TaskMarkedAsDoneEvent will be published: taskId={}",
                            savedTask.getId()
                    );
                }
                latestAssignmentUserId.ifPresent(userId -> {
                    LOGGER.info(
                            "Publishing TaskMarkedAsDoneEvent: taskId={}, userId={}",
                            savedTask.getId(),
                            userId
                    );
                    applicationEventPublisher.publishEvent(new TaskMarkedAsDoneEvent(savedTask.getId(), userId));
                });
            } catch (RuntimeException exception) {
                LOGGER.error(
                        "Failed while handling DONE transition for task: taskId={}, previousStatus={}, newStatus={}",
                        savedTask.getId(),
                        previousStatus,
                        savedTask.getStatus(),
                        exception
                );
                throw exception;
            }
        }

        return Optional.of(savedTask);
    }
}
