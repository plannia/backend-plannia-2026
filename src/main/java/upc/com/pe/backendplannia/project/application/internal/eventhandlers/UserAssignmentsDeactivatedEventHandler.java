package upc.com.pe.backendplannia.project.application.internal.eventhandlers;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.project.domain.model.commands.SyncTaskAssignmentStatusCommand;
import upc.com.pe.backendplannia.project.domain.services.TaskCommandService;
import upc.com.pe.backendplannia.shared.domain.model.events.UserAssignmentsDeactivatedEvent;

@Component
public class UserAssignmentsDeactivatedEventHandler {
    private final TaskCommandService taskCommandService;

    public UserAssignmentsDeactivatedEventHandler(TaskCommandService taskCommandService) {
        this.taskCommandService = taskCommandService;
    }

    @EventListener
    public void on(UserAssignmentsDeactivatedEvent event) {
        taskCommandService.handle(new SyncTaskAssignmentStatusCommand(event.taskIds()));
    }
}
