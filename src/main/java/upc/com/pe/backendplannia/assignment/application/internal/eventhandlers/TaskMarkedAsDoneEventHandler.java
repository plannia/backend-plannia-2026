package upc.com.pe.backendplannia.assignment.application.internal.eventhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.assignment.domain.model.commands.CompleteAssignmentCommand;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetLatestAssignmentByTaskIdQuery;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.LatestAssignmentSnapshot;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentCommandService;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentQueryService;
import upc.com.pe.backendplannia.shared.domain.model.events.TaskMarkedAsDoneEvent;

/**
 * When Project marks a task as DONE, complete its active assignment if one exists.
 */
@Component
public class TaskMarkedAsDoneEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskMarkedAsDoneEventHandler.class);

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
        LOGGER.info(
                "TaskMarkedAsDoneEvent received: taskId={}, userId={}",
                event.taskId(),
                event.userId()
        );

        boolean hasActiveAssignment = assignmentQueryService
                .handle(new GetLatestAssignmentByTaskIdQuery(event.taskId()))
                .map(LatestAssignmentSnapshot::isActive)
                .orElse(false);
        LOGGER.info(
                "TaskMarkedAsDoneEvent active assignment check: taskId={}, userId={}, hasActiveAssignment={}",
                event.taskId(),
                event.userId(),
                hasActiveAssignment
        );

        if (hasActiveAssignment) {
            try {
                LOGGER.info(
                        "Completing active assignment from task DONE event: taskId={}, userId={}",
                        event.taskId(),
                        event.userId()
                );
                assignmentCommandService.handle(new CompleteAssignmentCommand(event.taskId()));
            } catch (RuntimeException exception) {
                LOGGER.error(
                        "Failed to complete assignment from task DONE event: taskId={}, userId={}",
                        event.taskId(),
                        event.userId(),
                        exception
                );
                throw exception;
            }
        } else {
            LOGGER.info(
                    "Skipping assignment completion because latest assignment is not active: taskId={}, userId={}",
                    event.taskId(),
                    event.userId()
            );
        }
    }
}
