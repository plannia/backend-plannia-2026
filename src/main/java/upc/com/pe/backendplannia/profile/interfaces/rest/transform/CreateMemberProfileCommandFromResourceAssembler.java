package upc.com.pe.backendplannia.profile.interfaces.rest.transform;

import upc.com.pe.backendplannia.profile.domain.model.commands.CreateMemberProfileCommand;
import upc.com.pe.backendplannia.profile.interfaces.rest.resources.CreateMemberProfileResource;

public class CreateMemberProfileCommandFromResourceAssembler {
    public static CreateMemberProfileCommand toCommandFromResource(CreateMemberProfileResource resource) {
        return new CreateMemberProfileCommand(
                resource.userId(),
                resource.teamId(),
                resource.maxHours(),
                resource.abilities(),
                resource.interests()
        );
    }
}
