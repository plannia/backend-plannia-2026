package upc.com.pe.backendplannia.profile.interfaces.rest.resources;

public record CreateMemberProfileResource(
        Long userId,
        Long teamId,
        float maxHours,
        String abilities,
        String interests
) {
    public CreateMemberProfileResource {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User id must be a positive number");
        }
        if (teamId == null || teamId <= 0) {
            throw new IllegalArgumentException("Team id must be a positive number");
        }
        if (maxHours <= 0) {
            throw new IllegalArgumentException("Max hours must be greater than zero");
        }
        if (abilities == null || abilities.isBlank()) {
            throw new IllegalArgumentException("Abilities must not be null or blank");
        }
        if (interests == null || interests.isBlank()) {
            throw new IllegalArgumentException("Interests must not be null or blank");
        }
    }
}
