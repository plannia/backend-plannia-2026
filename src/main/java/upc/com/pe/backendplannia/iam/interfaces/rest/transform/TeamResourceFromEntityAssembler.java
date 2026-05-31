package upc.com.pe.backendplannia.iam.interfaces.rest.transform;

import upc.com.pe.backendplannia.iam.domain.model.aggregates.Team;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.TeamResource;

public class TeamResourceFromEntityAssembler {
    public static TeamResource toResourceFromEntity(Team team) {
        var members = team.getUsers().stream()
                .map(UserResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return new TeamResource(
                team.getId(),
                team.getName(),
                team.getCode(),
                members
        );
    }
}
