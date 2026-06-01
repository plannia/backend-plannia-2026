package upc.com.pe.backendplannia.assignment.infrastructure.acl;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.assignment.domain.services.TaskAssignmentPort;
import upc.com.pe.backendplannia.project.domain.model.commands.MarkTaskAsAssignedCommand;
import upc.com.pe.backendplannia.project.domain.services.TaskCommandService;

/**
 * Adaptador ACL: traduce la asignación de una tarea al comando público del contexto Project.
 */
@Service
public class ProjectContextTaskAssignmentAdapter implements TaskAssignmentPort {
    private final TaskCommandService taskCommandService;

    public ProjectContextTaskAssignmentAdapter(TaskCommandService taskCommandService) {
        this.taskCommandService = taskCommandService;
    }

    @Override
    public void markAsAssigned(Long taskId) {
        taskCommandService.handle(new MarkTaskAsAssignedCommand(taskId));
    }
}
