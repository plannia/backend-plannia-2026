package upc.com.pe.backendplannia.assignment.domain.services;

import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

/**
 * Puerto (ACL) para registrar la experiencia de un miembro en el contexto Profile cuando completa
 * una tarea. El embedding de la tarea lo provee el contexto Project (vía resolver) y viaja en el evento.
 */
public interface MemberExperiencePort {
    void recordExperience(Long userId, Long taskId, EmbeddingVector taskEmbedding);
}
