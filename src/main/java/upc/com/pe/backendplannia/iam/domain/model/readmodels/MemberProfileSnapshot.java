package upc.com.pe.backendplannia.iam.domain.model.readmodels;

public record MemberProfileSnapshot(
        Long id,
        Long userId,
        Long teamId,
        float maxHours,
        String abilities,
        String interests,
        float activeHours
) {
}
