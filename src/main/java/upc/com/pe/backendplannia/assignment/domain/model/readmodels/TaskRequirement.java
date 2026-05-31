package upc.com.pe.backendplannia.assignment.domain.model.readmodels;

import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

public record TaskRequirement(
        Long taskId,
        EmbeddingVector requirementsEmbedding,
        int estimatedHours,
        String urgency,
        String difficulty
) {
}
