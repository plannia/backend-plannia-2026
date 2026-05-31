package upc.com.pe.backendplannia.assignment.domain.model.readmodels;

import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

public record CandidateProfile(
        Long userId,
        EmbeddingVector embeddedAbilities,
        EmbeddingVector embeddedExperience,
        EmbeddingVector embeddedInterests,
        float activeHours,
        float maxHours
) {
    public float availableHours() {
        return maxHours - activeHours;
    }

    public boolean isAvailableFor(TaskRequirement taskRequirement) {
        return availableHours() >= taskRequirement.estimatedHours();
    }
}
