package upc.com.pe.backendplannia.assignment.infrastructure.acl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.assignment.domain.services.TeamLeadershipPort;
import upc.com.pe.backendplannia.iam.domain.model.queries.GetTeamByIdQuery;
import upc.com.pe.backendplannia.iam.domain.model.valueobjects.Role;
import upc.com.pe.backendplannia.iam.domain.services.TeamQueryService;

import java.util.Optional;

/**
 * Adaptador ACL: resuelve el líder del equipo delegando en la API pública de IAM
 * ({@link TeamQueryService}), sin depender de sus repositorios.
 *
 * <p>{@code @Transactional(readOnly = true)} porque {@code Team.getUsers()} es LAZY: mantiene la sesión
 * abierta mientras recorremos los usuarios para encontrar al de rol {@link Role#LEADER}.
 */
@Service
public class IamContextTeamLeadershipAdapter implements TeamLeadershipPort {
    private final TeamQueryService teamQueryService;

    public IamContextTeamLeadershipAdapter(TeamQueryService teamQueryService) {
        this.teamQueryService = teamQueryService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findLeaderUserId(Long teamId) {
        if (teamId == null) {
            return Optional.empty();
        }
        return teamQueryService.handle(new GetTeamByIdQuery(teamId))
                .flatMap(team -> team.getUsers().stream()
                        .filter(user -> user.getRole() == Role.LEADER)
                        .map(user -> user.getId())
                        .findFirst());
    }
}
