package upc.com.pe.backendplannia.project.infrastructure.acl;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import upc.com.pe.backendplannia.project.domain.services.TeamMemberPort;

import java.util.Optional;

/**
 * Adaptador ACL: traduce consultas de IAM al puerto {@link TeamMemberPort} del contexto Project.
 * Usa {@link UserRepository} directamente para evitar dependencia circular con {@code UserQueryService}.
 */
@Service
public class IamContextTeamMemberAdapter implements TeamMemberPort {
    private final UserRepository userRepository;

    public IamContextTeamMemberAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public Optional<Long> findTeamIdByUserId(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getTeam().getId());
    }

    @Override
    public Optional<String> findNameByUserId(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getName());
    }
}
