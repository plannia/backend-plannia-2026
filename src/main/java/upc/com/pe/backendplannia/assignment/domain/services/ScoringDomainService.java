package upc.com.pe.backendplannia.assignment.domain.services;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.TaskRequirement;
import upc.com.pe.backendplannia.assignment.domain.model.valueobjects.AssignmentScoreWeights;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.Comparator;
import java.util.List;

@Service
public class ScoringDomainService {
    // Los embeddings de este modelo tienen una "línea base" de similitud alta (~0.3) incluso entre
    // textos NO relacionados. Sin restar ese piso, cualquier experiencia previa —aunque sea de una
    // tarea de otro dominio— sumaba ~0.35*0.3 y podía decidir la asignación (p. ej. quien completó
    // un endpoint de backend ganaba una tarea de IA solo por "haber hecho algo antes"). Contamos solo
    // la parte de la similitud POR ENCIMA del piso, reescalada a [0,1]: la experiencia irrelevante
    // queda en 0 y solo pesa la experiencia realmente parecida a la tarea. Tunable.
    private static final float EXPERIENCE_RELEVANCE_FLOOR = 0.35f;

    public boolean meetsAvailabilityThreshold(CandidateProfile candidate, TaskRequirement task) {
        return candidate.availableHours() >= task.estimatedHours();
    }

    public float calculateScore(CandidateProfile candidate, TaskRequirement task) {
        var weights = AssignmentScoreWeights.defaults();
        float skillMatch = calculateSkillMatch(candidate, task);
        float experienceMatch = calculateExperienceMatch(candidate, task);
        float interestMatch = calculateInterestMatch(candidate, task);

        return skillMatch * weights.skillWeight()
                + experienceMatch * weights.experienceWeight()
                + interestMatch * weights.interestWeight();
    }

    public float calculateSkillMatch(CandidateProfile candidate, TaskRequirement task) {
        return calculateMatch(candidate.embeddedAbilities(), task.requirementsEmbedding());
    }

    public float calculateExperienceMatch(CandidateProfile candidate, TaskRequirement task) {
        var similarity = calculateMatch(candidate.embeddedExperience(), task.requirementsEmbedding());
        return relevanceAboveFloor(similarity, EXPERIENCE_RELEVANCE_FLOOR);
    }

    /** Similitud útil: descuenta la línea base y reescala a [0,1]. Debajo del piso → 0 (no relevante). */
    private float relevanceAboveFloor(float similarity, float floor) {
        if (similarity <= floor) {
            return 0f;
        }
        return (similarity - floor) / (1f - floor);
    }

    public float calculateInterestMatch(CandidateProfile candidate, TaskRequirement task) {
        return calculateMatch(candidate.embeddedInterests(), task.requirementsEmbedding());
    }

    public List<CandidateProfile> rankCandidates(List<CandidateProfile> candidates, TaskRequirement task) {
        // Precalculamos el score una vez por candidato: Comparator.comparing reevalúa la key en cada
        // comparación, y aquí esa key son varias similitudes coseno sobre vectores de ~768 dimensiones.
        return candidates.stream()
                .filter(candidate -> meetsAvailabilityThreshold(candidate, task))
                .map(candidate -> new ScoredCandidate(candidate, calculateScore(candidate, task)))
                .sorted(Comparator.comparing(ScoredCandidate::score).reversed())
                .map(ScoredCandidate::candidate)
                .toList();
    }

    private record ScoredCandidate(CandidateProfile candidate, float score) {
    }

    private float calculateMatch(EmbeddingVector candidateEmbedding, EmbeddingVector taskEmbedding) {
        if (candidateEmbedding == null || taskEmbedding == null) {
            return 0f;
        }
        if (candidateEmbedding.dimension() == 0 || taskEmbedding.dimension() == 0) {
            return 0f;
        }
        if (candidateEmbedding.dimension() != taskEmbedding.dimension()) {
            throw new IllegalArgumentException("Embedding vectors must have the same dimension");
        }
        return candidateEmbedding.cosineSimilarity(taskEmbedding);
    }
}
