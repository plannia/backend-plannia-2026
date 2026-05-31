package upc.com.pe.backendplannia.assignment.infrastructure.acl;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.assignment.domain.services.MemberWorkloadPort;
import upc.com.pe.backendplannia.profile.domain.model.commands.ReduceActiveHoursCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.UpdateActiveHoursCommand;
import upc.com.pe.backendplannia.profile.domain.services.MemberProfileCommandService;

/**
 * Adaptador ACL: traduce reservar/liberar carga a los comandos de Profile, reutilizando su lógica
 * de dominio ya validada (límites de horas) en lugar de mutar su aggregate directamente.
 */
@Service
public class ProfileContextMemberWorkloadAdapter implements MemberWorkloadPort {
    private final MemberProfileCommandService memberProfileCommandService;

    public ProfileContextMemberWorkloadAdapter(MemberProfileCommandService memberProfileCommandService) {
        this.memberProfileCommandService = memberProfileCommandService;
    }

    @Override
    public void reserveHours(Long userId, float hours) {
        memberProfileCommandService.handle(new UpdateActiveHoursCommand(userId, hours));
    }

    @Override
    public void releaseHours(Long userId, float hours) {
        memberProfileCommandService.handle(new ReduceActiveHoursCommand(userId, hours));
    }
}
