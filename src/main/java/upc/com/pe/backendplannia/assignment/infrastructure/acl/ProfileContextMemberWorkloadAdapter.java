package upc.com.pe.backendplannia.assignment.infrastructure.acl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileContextMemberWorkloadAdapter.class);

    private final MemberProfileCommandService memberProfileCommandService;

    public ProfileContextMemberWorkloadAdapter(MemberProfileCommandService memberProfileCommandService) {
        this.memberProfileCommandService = memberProfileCommandService;
    }

    @Override
    public void reserveHours(Long userId, float hours) {
        LOGGER.info("Profile ACL reserveHours requested: userId={}, hours={}", userId, hours);
        try {
            memberProfileCommandService.handle(new UpdateActiveHoursCommand(userId, hours));
        } catch (RuntimeException exception) {
            LOGGER.error("Profile ACL reserveHours failed: userId={}, hours={}", userId, hours, exception);
            throw exception;
        }
    }

    @Override
    public void releaseHours(Long userId, float hours) {
        LOGGER.info("Profile ACL releaseHours requested: userId={}, hours={}", userId, hours);
        try {
            memberProfileCommandService.handle(new ReduceActiveHoursCommand(userId, hours));
        } catch (RuntimeException exception) {
            LOGGER.error("Profile ACL releaseHours failed: userId={}, hours={}", userId, hours, exception);
            throw exception;
        }
    }
}
