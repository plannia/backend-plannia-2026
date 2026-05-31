package upc.com.pe.backendplannia.iam.domain.services;

import upc.com.pe.backendplannia.iam.domain.model.aggregates.Team;
import upc.com.pe.backendplannia.iam.domain.model.queries.GetTeamByIdQuery;

import java.util.Optional;

public interface TeamQueryService {
    Optional<Team> handle(GetTeamByIdQuery query);
}
