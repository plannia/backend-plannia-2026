package upc.com.pe.backendplannia.assignment.domain.model.readmodels;

import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.List;

/**
 * Perfil del candidato para el scoring. Skills e intereses van como un embedding del string completo.
 * {@code experienceItems} es un embedding POR tarea completada (no el promedio): el scoring toma el
 * MÁXIMO para no diluir la experiencia relevante con tareas de otros dominios. {@code embeddedExperience}
 * (promedio) queda como fallback para datos sin entradas individuales. Ver ScoringDomainService.
 */
public record CandidateProfile(
        Long userId,
        EmbeddingVector embeddedAbilities,
        EmbeddingVector embeddedExperience,
        EmbeddingVector embeddedInterests,
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
