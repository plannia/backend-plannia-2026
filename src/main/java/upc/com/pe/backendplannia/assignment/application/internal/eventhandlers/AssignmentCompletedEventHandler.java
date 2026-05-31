package upc.com.pe.backendplannia.assignment.application.internal.eventhandlers;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.assignment.domain.model.events.AssignmentCompletedEvent;
import upc.com.pe.backendplannia.assignment.domain.services.MemberExperiencePort;
import upc.com.pe.backendplannia.assignment.domain.services.MemberWorkloadPort;

/**
 * Aplica la política "al completar una tarea se actualiza la experiencia y se libera la carga del miembro".
 * Habla con Profile solo a través de puertos del dominio de Assignment (ACL), sin conocer sus tipos.
 */
@Component
public class AssignmentCompletedEventHandler {
    private final MemberExperiencePort memberExperiencePort;
    private final MemberWorkloadPort memberWorkloadPort;

    public AssignmentCompletedEventHandler(
            MemberExperiencePort memberExperiencePort,
            MemberWorkloadPort memberWorkloadPort
    ) {
        this.memberExperiencePort = memberExperiencePort;
        this.memberWorkloadPort = memberWorkloadPort;
    }

    // @EventListener corre síncrono dentro de la transacción del publisher: experiencia y carga
    // se confirman (o revierten) junto con el cambio de estado de la asignación.
    @EventListener
    public void on(AssignmentCompletedEvent event) {
        memberExperiencePort.recordExperience(
                event.userId(),
                event.taskId(),
                event.taskEmbedding()
        );
        memberWorkloadPort.releaseHours(
                event.userId(),
                event.estimatedHours()
        );
    }
}
