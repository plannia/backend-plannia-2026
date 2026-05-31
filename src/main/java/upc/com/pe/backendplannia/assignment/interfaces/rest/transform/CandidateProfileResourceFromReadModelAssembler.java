package upc.com.pe.backendplannia.assignment.interfaces.rest.transform;

import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;
import upc.com.pe.backendplannia.assignment.interfaces.rest.resources.CandidateProfileResource;

public class CandidateProfileResourceFromReadModelAssembler {
    public static CandidateProfileResource toResourceFromReadModel(CandidateProfile candidateProfile) {
        return new CandidateProfileResource(
                candidateProfile.userId(),
                candidateProfile.activeHours(),
                candidateProfile.maxHours(),
                candidateProfile.availableHours()
        );
    }
}
