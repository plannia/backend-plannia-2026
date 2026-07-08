package upc.com.pe.backendplannia.assignment.domain.services;

import org.junit.jupiter.api.Test;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.TaskRequirement;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.ArrayList;
import java.util.List;

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

    // Sin entradas de experiencia individuales: el scoring de experiencia cae al promedio (fallback).
    private CandidateProfile candidate(EmbeddingVector abilities, EmbeddingVector experience) {
        return new CandidateProfile(1L, abilities, experience, vec(1f, 0f), List.of(), 0f, 40f);
    }

    private TaskRequirement task(EmbeddingVector requirement) {
        return new TaskRequirement(9L, requirement, 4, "HIGH", "MEDIUM");
    }

    @Test
    void experienceBelowRelevanceFloorCountsAsZero() {
        // cos([1,0],[1,3]) = 0.316 < 0.42: experiencia irrelevante (otro dominio) NO suma.
        var candidate = candidate(vec(1f, 0f), vec(1f, 0f));
        var task = task(vec(1f, 3f));

        assertThat(service.calculateExperienceMatch(candidate, task)).isZero();
    }

    @Test
    void experienceAboveFloorIsRescaled() {
        // cos([1,1],[1,0]) = 0.707 -> (0.707 - 0.42) / (1 - 0.42) = 0.495
        var candidate = candidate(vec(1f, 0f), vec(1f, 1f));
        var task = task(vec(1f, 0f));

        assertThat(service.calculateExperienceMatch(candidate, task)).isCloseTo(0.495f, within(0.01f));
    }

    @Test
    void interestBelowRelevanceFloorCountsAsZero() {
        // cos([1,0],[1,4]) = 0.243 < 0.30: interés no relacionado (línea base) NO suma.
        var candidate = new CandidateProfile(1L, vec(1f, 0f), vec(1f, 0f), vec(1f, 0f), List.of(), 0f, 40f);
        var task = task(vec(1f, 4f));

        assertThat(service.calculateInterestMatch(candidate, task)).isZero();
    }

    @Test
    void interestAboveFloorIsRescaled() {
        // cos([1,1],[1,0]) = 0.707 -> (0.707 - 0.30) / (1 - 0.30) = 0.582
        var candidate = new CandidateProfile(1L, vec(1f, 0f), vec(1f, 0f), vec(1f, 1f), List.of(), 0f, 40f);
        var task = task(vec(1f, 0f));

        assertThat(service.calculateInterestMatch(candidate, task)).isCloseTo(0.582f, within(0.01f));
    }

    @Test
    void skillMatchStaysRawCosineNotFloored() {
        // La similitud 0.316 se mantiene tal cual para skill: el piso es solo de experiencia/interés.
        var candidate = candidate(vec(1f, 3f), vec(1f, 0f));
        var task = task(vec(1f, 0f));

        assertThat(service.calculateSkillMatch(candidate, task)).isCloseTo(0.316f, within(0.01f));
    }

    @Test
    void experienceMatchTakesTheMostSimilarCompletedTaskNotTheAverage() {
        // Dos tareas completadas: una relevante (cos 1) y otra irrelevante (cos 0). La experiencia es el
        // MÁXIMO (1 -> 1 tras piso), no el promedio (~0.71 -> 0.495). Prueba que no se diluye.
        var candidate = new CandidateProfile(1L, vec(1f, 0f), vec(1f, 0f), vec(1f, 0f),
                List.of(vec(1f, 0f), vec(0f, 1f)), 0f, 40f);
        var task = task(vec(1f, 0f));

        assertThat(service.calculateExperienceMatch(candidate, task)).isCloseTo(1f, within(0.001f));
    }
}
