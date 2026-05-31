package upc.com.pe.backendplannia.profile.domain.model.commands;

import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

public record AddExperienceEntryCommand(
        Long userId,
        Long taskId,
        EmbeddingVector taskEmbedding
) {
}
