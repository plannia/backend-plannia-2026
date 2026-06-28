package upc.com.pe.backendplannia.profile.interfaces.rest.resources;

/**
 * Null or blank fields mean no change and are ignored by the update command.
 */
public record UpdateMemberProfileResource(
        Float maxHours,
        String abilities,
        String interests
) {
    public UpdateMemberProfileResource {
        if (maxHours != null && maxHours <= 0) {
            throw new IllegalArgumentException("Max hours must be greater than zero");
        }
        abilities = normalizeOptionalText(abilities);
        interests = normalizeOptionalText(interests);
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
