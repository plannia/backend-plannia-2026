package upc.com.pe.backendplannia.assignment.interfaces.rest.transform;

import upc.com.pe.backendplannia.assignment.domain.model.readmodels.ScoredCandidate;
import upc.com.pe.backendplannia.assignment.interfaces.rest.resources.CandidateProfileResource;

public class CandidateProfileResourceFromReadModelAssembler {
    public static CandidateProfileResource toResourceFromReadModel(ScoredCandidate scoredCandidate) {
        var candidate = scoredCandidate.candidate();
        return new CandidateProfileResource(
                candidate.userId(),
                candidate.activeHours(),
                candidate.maxHours(),
                candidate.availableHours(),
                scoredCandidate.skillScore(),
                scoredCandidate.experienceScore(),
                scoredCandidate.interestScore(),
                scoredCandidate.totalScore()
        );
    }
}
