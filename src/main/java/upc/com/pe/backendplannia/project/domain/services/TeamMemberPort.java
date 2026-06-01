package upc.com.pe.backendplannia.project.domain.services;

import java.util.Optional;

/**
 * Puerto ACL hacia IAM para validar miembros de equipo sin acoplar el aggregate {@code User}.
 */
public interface TeamMemberPort {
    boolean existsById(Long userId);

    Optional<Long> findTeamIdByUserId(Long userId);

    Optional<String> findNameByUserId(Long userId);
}
