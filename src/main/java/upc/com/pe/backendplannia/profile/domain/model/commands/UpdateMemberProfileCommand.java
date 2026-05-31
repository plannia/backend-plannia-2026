package upc.com.pe.backendplannia.profile.domain.model.commands;

/**
 * Nullable fields are ignored during update, so only non-null values replace current profile data.
 */
public record UpdateMemberProfileCommand(
        Long userId,
        Float maxHours,
        String abilities,
        String interests
) {
}
