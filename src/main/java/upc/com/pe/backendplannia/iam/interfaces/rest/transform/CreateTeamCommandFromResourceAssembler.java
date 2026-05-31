package upc.com.pe.backendplannia.iam.interfaces.rest.transform;

import upc.com.pe.backendplannia.iam.domain.model.commands.CreateTeamCommand;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.CreateTeamResource;

public class CreateTeamCommandFromResourceAssembler {
    public static CreateTeamCommand toCommandFromResource(CreateTeamResource resource) {
        return new CreateTeamCommand(
                resource.teamName(),
                resource.email(),
                resource.leaderName(),
                resource.password()
        );
    }
}
