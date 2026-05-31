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
        return calculateMatch(candidate.embeddedExperience(), task.requirementsEmbedding());
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
