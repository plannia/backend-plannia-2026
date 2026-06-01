package upc.com.pe.backendplannia.project.infrastructure.acl;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.iam.domain.model.queries.GetTeamByIdQuery;
import upc.com.pe.backendplannia.iam.domain.services.TeamQueryService;
import upc.com.pe.backendplannia.project.domain.services.TeamExistencePort;

/**
 * Adaptador ACL: traduce consultas de IAM al puerto {@link TeamExistencePort} del contexto Project.
 */
@Service
public class IamContextTeamExistenceAdapter implements TeamExistencePort {
    private final TeamQueryService teamQueryService;

    public IamContextTeamExistenceAdapter(TeamQueryService teamQueryService) {
        this.teamQueryService = teamQueryService;
    }

    @Override
    public boolean existsById(Long teamId) {
        return teamQueryService.handle(new GetTeamByIdQuery(teamId)).isPresent();
    }
}
