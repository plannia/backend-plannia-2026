package upc.com.pe.backendplannia.iam.interfaces.rest.resources;

import upc.com.pe.backendplannia.iam.domain.model.valueobjects.Role;

public record UserResource(
        Long id,
        String name,
        String email,
        String position,
        Role role
) {
}
