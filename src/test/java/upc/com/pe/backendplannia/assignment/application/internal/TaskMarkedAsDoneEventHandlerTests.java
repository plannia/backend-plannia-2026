package upc.com.pe.backendplannia.assignment.application.internal;

import org.junit.jupiter.api.Test;
import upc.com.pe.backendplannia.assignment.application.internal.eventhandlers.TaskMarkedAsDoneEventHandler;
import upc.com.pe.backendplannia.assignment.domain.model.commands.CompleteAssignmentCommand;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetLatestAssignmentByTaskIdQuery;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.LatestAssignmentSnapshot;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentCommandService;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentQueryService;
import upc.com.pe.backendplannia.shared.domain.model.events.TaskMarkedAsDoneEvent;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskMarkedAsDoneEventHandlerTests {
    private static final Long TASK_ID = 501L;
    private static final Long USER_ID = 101L;

    private final AssignmentCommandService assignmentCommandService = mock(AssignmentCommandService.class);
    private final AssignmentQueryService assignmentQueryService = mock(AssignmentQueryService.class);
    private final TaskMarkedAsDoneEventHandler handler =
            new TaskMarkedAsDoneEventHandler(assignmentCommandService, assignmentQueryService);

    @Test
    void onTaskMarkedAsDoneCompletesActiveAssignment() {
        when(assignmentQueryService.handle(any(GetLatestAssignmentByTaskIdQuery.class)))
                .thenReturn(Optional.of(new LatestAssignmentSnapshot(USER_ID, true)));

        handler.on(new TaskMarkedAsDoneEvent(TASK_ID, USER_ID));

        // Completar dispara AssignmentCompletedEvent, que libera la carga y registra la experiencia.
        verify(assignmentCommandService).handle(new CompleteAssignmentCommand(TASK_ID));
    }

    @Test
    void onTaskMarkedAsDoneDoesNothingWhenLatestAssignmentIsNotActive() {
        when(assignmentQueryService.handle(any(GetLatestAssignmentByTaskIdQuery.class)))
                .thenReturn(Optional.of(new LatestAssignmentSnapshot(USER_ID, false)));

        handler.on(new TaskMarkedAsDoneEvent(TASK_ID, USER_ID));

        verify(assignmentCommandService, never()).handle(any(CompleteAssignmentCommand.class));
    }

    @Test
    void onTaskMarkedAsDoneDoesNothingWhenNoAssignmentExists() {
        when(assignmentQueryService.handle(any(GetLatestAssignmentByTaskIdQuery.class)))
                .thenReturn(Optional.empty());

        handler.on(new TaskMarkedAsDoneEvent(TASK_ID, USER_ID));

        verify(assignmentCommandService, never()).handle(any(CompleteAssignmentCommand.class));
    }
}
