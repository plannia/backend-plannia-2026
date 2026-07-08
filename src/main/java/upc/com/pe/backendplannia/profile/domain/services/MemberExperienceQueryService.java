package upc.com.pe.backendplannia.profile.domain.services;

import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.List;

/**
 * API pública de Profile para leer la experiencia "cruda": un embedding por cada tarea que el miembro
 * completó (no el promedio). El scoring de Assignment toma el máximo para no diluir la experiencia
 * relevante con tareas de otros dominios.
 */
public interface MemberExperienceQueryService {
    List<EmbeddingVector> findExperienceEmbeddings(Long userId);
}
