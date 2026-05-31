package upc.com.pe.backendplannia.assignment.domain.model.events;

import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

// estimatedHours y taskEmbedding viajan en el evento porque viven en el contexto Project (vía resolver):
// el handler los necesita para liberar la carga del miembro y registrar su experiencia sin volver a resolverlos.
public record AssignmentCompletedEvent(
        Long taskId,
        Long userId,
        int estimatedHours,
        EmbeddingVector taskEmbedding
) {
}
