package upc.com.pe.backendplannia.assignment.application.internal.eventhandlers;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.assignment.application.internal.outboundservices.TaskRequirementGateway;
import upc.com.pe.backendplannia.assignment.domain.services.MemberExperiencePort;
import upc.com.pe.backendplannia.shared.domain.model.events.TaskMarkedAsDoneEvent;

@Component
public class TaskMarkedAsDoneEventHandler {
    private final TaskRequirementGateway taskRequirementGateway;
    private final MemberExperiencePort memberExperiencePort;

    public TaskMarkedAsDoneEventHandler(
            TaskRequirementGateway taskRequirementGateway,
            MemberExperiencePort memberExperiencePort
    ) {
        this.taskRequirementGateway = taskRequirementGateway;
        this.memberExperiencePort = memberExperiencePort;
    }

    @EventListener
    public void on(TaskMarkedAsDoneEvent event) {
        var taskRequirement = taskRequirementGateway.requireByTaskId(event.taskId());
        memberExperiencePort.recordExperience(
                event.userId(),
                event.taskId(),
                taskRequirement.requirementsEmbedding()
        );
    }
}
