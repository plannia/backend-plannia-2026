package upc.com.pe.backendplannia.assignment.infrastructure.acl;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.BacklogTask;
import upc.com.pe.backendplannia.assignment.domain.services.UnassignedTaskPort;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Task;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Difficulty;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Priority;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories.TaskRepository;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Adaptador ACL: traduce las tareas de Project al read model {@link BacklogTask} de Assignment.
 * Filtra a las ASIGNABLES (sin asignar y no terminadas) y mapea los enums de prioridad/dificultad de
 * Project a rangos numéricos, para que Assignment ordene sin importar los tipos de Project.
 */
@Service
public class ProjectContextUnassignedTaskAdapter implements UnassignedTaskPort {
    private static final Set<Status> TERMINAL_STATUSES = EnumSet.of(Status.DONE, Status.CANCELLED);

    private final TaskRepository taskRepository;

    public ProjectContextUnassignedTaskAdapter(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public List<BacklogTask> findUnassignedByTeamId(Long teamId) {
        return toBacklog(taskRepository.findByCategory_TeamId_Id(teamId));
    }

    @Override
    public List<BacklogTask> findUnassignedByCategoryId(Long categoryId) {
        return toBacklog(taskRepository.findByCategory_Id(categoryId));
    }

    private List<BacklogTask> toBacklog(List<Task> tasks) {
        return tasks.stream()
                .filter(this::isAssignable)
                .map(this::toBacklogTask)
                .toList();
    }

    private boolean isAssignable(Task task) {
        return !task.isAssigned() && !TERMINAL_STATUSES.contains(task.getStatus());
    }

    private BacklogTask toBacklogTask(Task task) {
        return new BacklogTask(
                task.getId(),
                priorityRank(task.getPriority()),
                difficultyRank(task.getDifficulty()),
                task.getLimitDate()
        );
    }

    private int priorityRank(Priority priority) {
        return switch (priority) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private int difficultyRank(Difficulty difficulty) {
        return switch (difficulty) {
            case HARD -> 3;
            case MEDIUM -> 2;
            case EASY -> 1;
        };
    }
}
