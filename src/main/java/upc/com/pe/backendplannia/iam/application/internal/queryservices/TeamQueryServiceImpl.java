package upc.com.pe.backendplannia.iam.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.iam.domain.model.aggregates.Team;
import upc.com.pe.backendplannia.iam.domain.model.queries.GetTeamByIdQuery;
import upc.com.pe.backendplannia.iam.domain.services.TeamQueryService;
import upc.com.pe.backendplannia.iam.infrastructure.persistence.jpa.repositories.TeamRepository;

import java.util.Optional;

@Service
public class TeamQueryServiceImpl implements TeamQueryService {
    private final TeamRepository teamRepository;

    public TeamQueryServiceImpl(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Team> handle(GetTeamByIdQuery query) {
        return teamRepository.findWithUsersById(query.teamId());
    }
}
