package upc.com.pe.backendplannia.assignment.domain.model.readmodels;

import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.List;

/**
 * Perfil del candidato para el scoring. Los embeddings "single" (abilities/interests/experience) son
 * el promedio/string-completo y quedan como fallback para perfiles viejos. Las listas *Items son un
 * embedding por ítem (por habilidad, por interés, por tarea completada): el scoring toma el MÁXIMO
 * para no diluir lo relevante. Ver ScoringDomainService.
 */
public record CandidateProfile(
        Long userId,
        EmbeddingVector embeddedAbilities,
        EmbeddingVector embeddedExperience,
        EmbeddingVector embeddedInterests,
        List<EmbeddingVector> abilityItems,
        List<EmbeddingVector> interestItems,
        List<EmbeddingVector> experienceItems,
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
