package upc.com.pe.backendplannia.profile.application.internal.eventhandlers;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.profile.domain.model.commands.CreateDefaultMemberProfileCommand;
import upc.com.pe.backendplannia.profile.domain.services.MemberProfileCommandService;
import upc.com.pe.backendplannia.shared.domain.model.events.MemberRegisteredEvent;

/**
 * Al registrarse un miembro (IAM), Profile le crea automáticamente un perfil base.
 *
 * Es @EventListener SÍNCRONO a propósito: corre dentro de la transacción del SignUp y NO hace I/O
 * externo (el perfil base no genera embeddings), así que usuario y perfil se confirman juntos
 * (atómico). Nunca queda un miembro sin perfil.
 */
@Component
public class MemberRegisteredEventHandler {
    private final MemberProfileCommandService memberProfileCommandService;

    public MemberRegisteredEventHandler(MemberProfileCommandService memberProfileCommandService) {
        this.memberProfileCommandService = memberProfileCommandService;
    }

    @EventListener
    public void on(MemberRegisteredEvent event) {
        memberProfileCommandService.handle(
                new CreateDefaultMemberProfileCommand(event.userId(), event.teamId()));
    }
}
