package upc.com.pe.backendplannia.project.application.internal.queryservices;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Task;
import upc.com.pe.backendplannia.project.domain.model.queries.GetTasksByFilterQuery;
import upc.com.pe.backendplannia.project.domain.model.queries.GetTasksForDashboardQuery;
import upc.com.pe.backendplannia.project.domain.model.queries.GetTasksForPlannerQuery;
import upc.com.pe.backendplannia.project.domain.model.queries.GetTaskStatusCountsByLatestAssignmentUserIdQuery;
import upc.com.pe.backendplannia.project.domain.model.readmodels.DashboardTaskItem;
import upc.com.pe.backendplannia.project.domain.model.readmodels.TaskStatusCounts;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Difficulty;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Priority;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;
import upc.com.pe.backendplannia.project.domain.services.AssignmentActivityPort;
import upc.com.pe.backendplannia.project.domain.services.TaskQueryService;
import upc.com.pe.backendplannia.project.domain.services.TeamExistencePort;
import upc.com.pe.backendplannia.project.domain.services.TeamMemberPort;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories.TaskRepository;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.specifications.TaskSpecifications;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class TaskQueryServiceImpl implements TaskQueryService {
    private final TaskRepository taskRepository;
    private final TeamExistencePort teamExistencePort;
    private final AssignmentActivityPort assignmentActivityPort;
    private final TeamMemberPort teamMemberPort;

    public TaskQueryServiceImpl(
            TaskRepository taskRepository,
            TeamExistencePort teamExistencePort,
            AssignmentActivityPort assignmentActivityPort,
            TeamMemberPort teamMemberPort
    ) {
        this.taskRepository = taskRepository;
        this.teamExistencePort = teamExistencePort;
        this.assignmentActivityPort = assignmentActivityPort;
        this.teamMemberPort = teamMemberPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> handle(GetTasksByFilterQuery query) {
        if (!teamExistencePort.existsById(query.teamId())) {
            throw new IllegalArgumentException("Team not found");
        }

        Specification<Task> specification = TaskSpecifications.byTeamId(query.teamId());

        if (hasText(query.title())) {
            specification = specification.and(TaskSpecifications.titleContains(query.title()));
        }
        if (hasText(query.description())) {
            specification = specification.and(TaskSpecifications.descriptionContains(query.description()));
        }
        if (hasText(query.priority())) {
            specification = specification.and(TaskSpecifications.hasPriority(parsePriority(query.priority())));
        }
        if (hasText(query.difficulty())) {
            specification = specification.and(TaskSpecifications.hasDifficulty(parseDifficulty(query.difficulty())));
        }
        if (hasText(query.status())) {
            specification = specification.and(TaskSpecifications.hasStatus(parseStatus(query.status())));
        }
        if (hasText(query.categoryId())) {
            specification = specification.and(TaskSpecifications.hasCategoryId(parseLong(query.categoryId(), "categoryId")));
        }
        if (hasText(query.userId())) {
            var taskIds = assignmentActivityPort.findTaskIdsByLatestAssignmentUserId(
                    parseLong(query.userId(), "userId")
            );
            if (taskIds.isEmpty()) {
                return List.of();
            }
            specification = specification.and(TaskSpecifications.taskIdIn(taskIds));
        }

        return taskRepository.findAll(specification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardTaskItem> handle(GetTasksForDashboardQuery query) {
        if (!teamExistencePort.existsById(query.teamId())) {
            throw new IllegalArgumentException("Team not found");
        }

        var specification = TaskSpecifications.byTeamId(query.teamId())
                .and(TaskSpecifications.hasStatus(Status.IN_PROGRESS))
                .and(TaskSpecifications.isAssigned(true));

        return taskRepository.findAll(specification).stream()
                .map(this::toDashboardTaskItem)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardTaskItem> handle(GetTasksForPlannerQuery query) {
        if (!teamExistencePort.existsById(query.teamId())) {
            throw new IllegalArgumentException("Team not found");
        }

        var baseSpec = TaskSpecifications.byTeamId(query.teamId())
                .and(TaskSpecifications.isAssigned(true));

        var inProgressTasks = taskRepository.findAll(
                baseSpec.and(TaskSpecifications.hasStatus(Status.IN_PROGRESS))
        );
        var doneTasks = taskRepository.findAll(
                baseSpec.and(TaskSpecifications.hasStatus(Status.DONE))
        ).stream()
                .filter(task -> task.getStartTime() != null && task.getEndTime() != null)
                .toList();

        return Stream.concat(inProgressTasks.stream(), doneTasks.stream())
                .map(this::toDashboardTaskItem)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskStatusCounts handle(GetTaskStatusCountsByLatestAssignmentUserIdQuery query) {
        var taskIds = assignmentActivityPort.findTaskIdsByLatestAssignmentUserId(query.userId());
        if (taskIds.isEmpty()) {
            return new TaskStatusCounts(0, 0, 0);
        }

        var tasks = taskRepository.findAllById(taskIds);
        return new TaskStatusCounts(
                tasks.stream().filter(task -> task.getStatus() == Status.TO_DO).count(),
                tasks.stream().filter(task -> task.getStatus() == Status.IN_PROGRESS).count(),
                tasks.stream().filter(task -> task.getStatus() == Status.DONE).count()
        );
    }

    private Optional<DashboardTaskItem> toDashboardTaskItem(Task task) {
        return assignmentActivityPort.findLatestAssignmentUserId(task.getId())
                .flatMap(userId -> teamMemberPort.findNameByUserId(userId)
                        .map(userName -> new DashboardTaskItem(
                                userId,
                                userName,
                                task.getId(),
                                task.getTitle(),
                                task.getStatus(),
                                task.getStartTime(),
                                task.getEndTime(),
                                task.getHours(),
                                task.getCategory().getId(),
                                task.getCategory().getName()
                        )));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Long parseLong(String value, String fieldName) {
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid " + fieldName);
        }
    }

    private static Priority parsePriority(String priority) {
        try {
            return Priority.valueOf(priority.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid priority");
        }
    }

    private static Difficulty parseDifficulty(String difficulty) {
        try {
            return Difficulty.valueOf(difficulty.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid difficulty");
        }
    }

    private static Status parseStatus(String status) {
        try {
            return Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid status");
        }
    }
}
