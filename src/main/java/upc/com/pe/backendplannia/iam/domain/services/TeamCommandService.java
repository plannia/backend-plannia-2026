package upc.com.pe.backendplannia.iam.domain.services;

import upc.com.pe.backendplannia.iam.domain.model.aggregates.Team;
import upc.com.pe.backendplannia.iam.domain.model.commands.CreateTeamCommand;

import java.util.Optional;

public interface TeamCommandService {
    Optional<Team> handle(CreateTeamCommand command);
}
