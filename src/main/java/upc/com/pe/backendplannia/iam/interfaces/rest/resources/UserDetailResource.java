package upc.com.pe.backendplannia.iam.interfaces.rest.resources;

import upc.com.pe.backendplannia.iam.domain.model.valueobjects.Role;

public record UserDetailResource(
        Long id,
        String name,
        String email,
        String position,
        Role role,
        Long teamId,
        UserDetailProfileResource profile,
        UserTaskStatusCountsResource taskStatusCounts
) {
}
