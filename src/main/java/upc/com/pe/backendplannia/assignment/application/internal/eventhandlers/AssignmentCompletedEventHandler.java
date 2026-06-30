package upc.com.pe.backendplannia.assignment.application.internal.eventhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.assignment.domain.model.events.AssignmentCompletedEvent;
import upc.com.pe.backendplannia.assignment.domain.services.MemberExperiencePort;
import upc.com.pe.backendplannia.assignment.domain.services.MemberWorkloadPort;

/**
 * Applies the policy that completing a task records experience and releases workload.
 */
@Component
public class AssignmentCompletedEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentCompletedEventHandler.class);

    private final MemberExperiencePort memberExperiencePort;
    private final MemberWorkloadPort memberWorkloadPort;

    public AssignmentCompletedEventHandler(
            MemberExperiencePort memberExperiencePort,
            MemberWorkloadPort memberWorkloadPort
    ) {
        this.memberExperiencePort = memberExperiencePort;
        this.memberWorkloadPort = memberWorkloadPort;
    }

    @EventListener
    public void on(AssignmentCompletedEvent event) {
        LOGGER.info(
                "AssignmentCompletedEvent received: taskId={}, userId={}, estimatedHours={}, embeddingDim={}",
                event.taskId(),
                event.userId(),
                event.estimatedHours(),
                event.taskEmbedding().dimension()
        );

        try {
            LOGGER.info(
                    "Recording member experience from completed assignment: taskId={}, userId={}, embeddingDim={}",
                    event.taskId(),
                    event.userId(),
                    event.taskEmbedding().dimension()
            );
            memberExperiencePort.recordExperience(
                    event.userId(),
                    event.taskId(),
                    event.taskEmbedding()
            );
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Failed to record member experience from completed assignment: taskId={}, userId={}, embeddingDim={}",
                    event.taskId(),
                    event.userId(),
                    event.taskEmbedding().dimension(),
                    exception
            );
            throw exception;
        }

        try {
            LOGGER.info(
                    "Releasing member workload from completed assignment: taskId={}, userId={}, hours={}",
                    event.taskId(),
                    event.userId(),
                    event.estimatedHours()
            );
            memberWorkloadPort.releaseHours(
                    event.userId(),
                    event.estimatedHours()
            );
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Failed to release member workload from completed assignment: taskId={}, userId={}, hours={}",
                    event.taskId(),
                    event.userId(),
                    event.estimatedHours(),
                    exception
            );
            throw exception;
        }
    }
}
