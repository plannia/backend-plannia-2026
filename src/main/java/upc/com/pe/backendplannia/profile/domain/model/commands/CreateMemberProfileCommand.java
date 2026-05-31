package upc.com.pe.backendplannia.profile.domain.model.commands;

public record CreateMemberProfileCommand(
        Long userId,
        Long teamId,
        float maxHours,
        String abilities,
        String interests
) {
}
