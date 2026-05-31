package upc.com.pe.backendplannia.profile.interfaces.rest.transform;

import upc.com.pe.backendplannia.profile.domain.model.commands.UpdateMemberProfileCommand;
import upc.com.pe.backendplannia.profile.interfaces.rest.resources.UpdateMemberProfileResource;

public class UpdateMemberProfileCommandFromResourceAssembler {
    public static UpdateMemberProfileCommand toCommandFromResource(Long userId, UpdateMemberProfileResource resource) {
        return new UpdateMemberProfileCommand(
                userId,
                resource.maxHours(),
                resource.abilities(),
                resource.interests()
        );
    }
}
