package upc.com.pe.backendplannia.assignment.application.internal.eventhandlers;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.assignment.domain.model.commands.CompleteAssignmentCommand;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetLatestAssignmentByTaskIdQuery;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.LatestAssignmentSnapshot;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentCommandService;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentQueryService;
import upc.com.pe.backendplannia.shared.domain.model.events.TaskMarkedAsDoneEvent;

/**
 * Cuando Project marca una tarea como DONE, completamos su asignación activa (si la hay).
 * La finalización pasa así por una sola política: completar libera la carga del miembro y registra su
 * experiencia (vía AssignmentCompletedEvent), igual que el endpoint /assignments/complete. Antes este
 * handler solo registraba experiencia, dejando la carga (activeHours) reservada para siempre.
 */
@Component
public class TaskMarkedAsDoneEventHandler {
    private final AssignmentCommandService assignmentCommandService;
    private final AssignmentQueryService assignmentQueryService;

    public TaskMarkedAsDoneEventHandler(
            AssignmentCommandService assignmentCommandService,
            AssignmentQueryService assignmentQueryService
    ) {
        this.assignmentCommandService = assignmentCommandService;
        this.assignmentQueryService = assignmentQueryService;
    }

    @EventListener
    public void on(TaskMarkedAsDoneEvent event) {
        // Solo completamos si la última asignación sigue activa: si ya se completó/desactivó no hay nada
        // que liberar. Además evita que CompleteAssignmentCommand lance y revierta la actualización de la
        // tarea en Project (este handler corre dentro de esa transacción).
        boolean hasActiveAssignment = assignmentQueryService
                .handle(new GetLatestAssignmentByTaskIdQuery(event.taskId()))
                .map(LatestAssignmentSnapshot::isActive)
                .orElse(false);

        if (hasActiveAssignment) {
            assignmentCommandService.handle(new CompleteAssignmentCommand(event.taskId()));
        }
    }
}
