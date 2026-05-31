package upc.com.pe.backendplannia.profile.interfaces.rest.resources;

public record MemberProfileResource(
        Long id,
        Long userId,
        Long teamId,
        float maxHours,
        String abilities,
        String interests,
        float activeHours
) {
}
