package upc.com.pe.backendplannia.assignment.application.internal.eventhandlers;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.assignment.domain.model.commands.DeactivateUserAssignmentsCommand;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentCommandService;
import upc.com.pe.backendplannia.shared.domain.model.events.UserAssignmentsDeactivatedEvent;
import upc.com.pe.backendplannia.shared.domain.model.events.UserDeletedEvent;

@Component
public class UserDeletedAssignmentEventHandler {
    private final AssignmentCommandService assignmentCommandService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public UserDeletedAssignmentEventHandler(
            AssignmentCommandService assignmentCommandService,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.assignmentCommandService = assignmentCommandService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener
    public void on(UserDeletedEvent event) {
        var affectedTaskIds = assignmentCommandService.handle(new DeactivateUserAssignmentsCommand(event.userId()));
        if (!affectedTaskIds.isEmpty()) {
            applicationEventPublisher.publishEvent(new UserAssignmentsDeactivatedEvent(affectedTaskIds));
        }
    }
}
