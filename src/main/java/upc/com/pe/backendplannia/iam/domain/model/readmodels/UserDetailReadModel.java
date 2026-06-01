package upc.com.pe.backendplannia.iam.domain.model.readmodels;

import upc.com.pe.backendplannia.iam.domain.model.valueobjects.Role;

public record UserDetailReadModel(
        Long id,
        String name,
        String email,
        String position,
        Role role,
        Long teamId,
        MemberProfileSnapshot profile,
        UserTaskStatusCounts taskStatusCounts
) {
}
