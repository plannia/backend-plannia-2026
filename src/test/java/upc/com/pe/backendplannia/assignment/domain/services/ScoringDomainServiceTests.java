package upc.com.pe.backendplannia.assignment.domain.services;

import org.junit.jupiter.api.Test;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.TaskRequirement;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ScoringDomainServiceTests {
    private final ScoringDomainService service = new ScoringDomainService();

    private static EmbeddingVector vec(float... xs) {
        var list = new ArrayList<Float>();
        for (var x : xs) {
            list.add(x);
        }
        return EmbeddingVector.of(list);
    }

    private CandidateProfile candidate(EmbeddingVector abilities, EmbeddingVector experience) {
        return new CandidateProfile(1L, abilities, experience, vec(1f, 0f), 0f, 40f);
    }

    private TaskRequirement task(EmbeddingVector requirement) {
        return new TaskRequirement(9L, requirement, 4, "HIGH", "MEDIUM");
    }

    @Test
    void experienceBelowRelevanceFloorCountsAsZero() {
        // cos([1,0],[1,3]) = 0.316 < 0.35: experiencia irrelevante (otro dominio) NO suma.
        var candidate = candidate(vec(1f, 0f), vec(1f, 0f));
        var task = task(vec(1f, 3f));

        assertThat(service.calculateExperienceMatch(candidate, task)).isZero();
    }

    @Test
    void experienceAboveFloorIsRescaled() {
        // cos([1,1],[1,0]) = 0.707 -> (0.707 - 0.35) / (1 - 0.35) = 0.549
        var candidate = candidate(vec(1f, 0f), vec(1f, 1f));
        var task = task(vec(1f, 0f));

        assertThat(service.calculateExperienceMatch(candidate, task)).isCloseTo(0.549f, within(0.01f));
    }

    @Test
    void skillMatchStaysRawCosineNotFloored() {
        // La misma similitud 0.316 se mantiene tal cual para skill: el piso es solo de experiencia (fix #1).
        var candidate = candidate(vec(1f, 3f), vec(1f, 0f));
        var task = task(vec(1f, 0f));

        assertThat(service.calculateSkillMatch(candidate, task)).isCloseTo(0.316f, within(0.01f));
    }
}
