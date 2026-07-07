package upc.com.pe.backendplannia.assignment.domain.services;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.ScoredCandidate;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.TaskRequirement;
import upc.com.pe.backendplannia.assignment.domain.model.valueobjects.AssignmentScoreWeights;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.Comparator;
import java.util.List;

@Service
public class ScoringDomainService {
    // Los embeddings de este modelo tienen una "línea base" de similitud alta incluso entre textos NO
    // relacionados (experiencia cross-dominio ~0.40; intereses no relacionados ~0.22). Sin restar ese
    // piso, cualquier experiencia/interés previo sumaba por la pura línea base y podía decidir la
    // asignación. Contamos solo la parte de la similitud POR ENCIMA del piso, reescalada a [0,1]: lo
    // irrelevante queda en 0 y solo pesa lo realmente parecido a la tarea. Los pisos difieren porque
    // la distribución de cosenos difiere entre señales (tareas completadas vs. strings de interés).
    // Tunables.
    private static final float EXPERIENCE_RELEVANCE_FLOOR = 0.42f;
    private static final float INTEREST_RELEVANCE_FLOOR = 0.30f;

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
        var similarity = calculateMatch(candidate.embeddedInterests(), task.requirementsEmbedding());
        return relevanceAboveFloor(similarity, INTEREST_RELEVANCE_FLOOR);
    }

    // Ranking con el desglose de cada sub-score (skill/experiencia/interés) + total. Precalculamos una
    // vez por candidato: Comparator.comparing reevalúa la key en cada comparación, y esa key son varias
    // similitudes coseno sobre vectores grandes.
    public List<ScoredCandidate> rankCandidatesWithScores(List<CandidateProfile> candidates, TaskRequirement task) {
        var weights = AssignmentScoreWeights.defaults();
        return candidates.stream()
                .filter(candidate -> meetsAvailabilityThreshold(candidate, task))
                .map(candidate -> {
                    float skill = calculateSkillMatch(candidate, task);
                    float experience = calculateExperienceMatch(candidate, task);
                    float interest = calculateInterestMatch(candidate, task);
                    float total = skill * weights.skillWeight()
                            + experience * weights.experienceWeight()
                            + interest * weights.interestWeight();
                    return new ScoredCandidate(candidate, skill, experience, interest, total);
                })
                .sorted(Comparator.comparing(ScoredCandidate::totalScore).reversed())
                .toList();
    }

    public List<CandidateProfile> rankCandidates(List<CandidateProfile> candidates, TaskRequirement task) {
        return rankCandidatesWithScores(candidates, task).stream()
                .map(ScoredCandidate::candidate)
                .toList();
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
