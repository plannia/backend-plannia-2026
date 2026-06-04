package upc.com.pe.backendplannia.profile.application.internal.eventhandlers;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.profile.domain.model.commands.DeleteMemberProfileCommand;
import upc.com.pe.backendplannia.profile.domain.services.MemberProfileCommandService;
import upc.com.pe.backendplannia.shared.domain.model.events.UserDeletedEvent;

/**
 * Al borrarse un usuario (IAM), Profile elimina su perfil y entradas de experiencia, para que no
 * quede un perfil huérfano que siga apareciendo como candidato.
 *
 * Es uno de los listeners de UserDeletedEvent (fan-out): Assignment desactiva asignaciones, Project
 * limpia categorías y Profile borra el perfil. Síncrono y sin I/O externo → atómico con el borrado.
 */
@Component
public class UserDeletedProfileEventHandler {
    private final MemberProfileCommandService memberProfileCommandService;

    public UserDeletedProfileEventHandler(MemberProfileCommandService memberProfileCommandService) {
        this.memberProfileCommandService = memberProfileCommandService;
    }

    @EventListener
    public void on(UserDeletedEvent event) {
        memberProfileCommandService.handle(new DeleteMemberProfileCommand(event.userId()));
    }
}
