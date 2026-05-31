package upc.com.pe.backendplannia.profile.interfaces.rest.resources;

/**
 * Null fields mean no change and are ignored by the update command.
 */
public record UpdateMemberProfileResource(
        Float maxHours,
        String abilities,
        String interests
) {
    // Null = sin cambio; pero si un campo viene presente debe ser válido (evita embeddings de texto vacío).
    public UpdateMemberProfileResource {
        if (maxHours != null && maxHours <= 0) {
            throw new IllegalArgumentException("Max hours must be greater than zero");
        }
        if (abilities != null && abilities.isBlank()) {
            throw new IllegalArgumentException("Abilities must not be blank");
        }
        if (interests != null && interests.isBlank()) {
            throw new IllegalArgumentException("Interests must not be blank");
        }
    }
}
