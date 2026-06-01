package upc.com.pe.backendplannia.iam.interfaces.rest.resources;

import upc.com.pe.backendplannia.iam.domain.model.valueobjects.Role;

public record UserDetailProfileResource(
        Long id,
        Long userId,
        Long teamId,
        float maxHours,
        String abilities,
        String interests,
        float activeHours
) {
}
