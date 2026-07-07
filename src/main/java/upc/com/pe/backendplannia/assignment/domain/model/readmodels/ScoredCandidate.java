package upc.com.pe.backendplannia.assignment.domain.model.readmodels;

/**
 * Un candidato con el desglose de su score. skillScore/experienceScore/interestScore son los
 * "matches" (similitud coseno tal como los usa el scoring: experienceScore ya viene post-piso de
 * relevancia); totalScore es la suma ponderada. Lo expone /recommend para que el líder vea qué se
 * calculó contra qué.
 */
public record ScoredCandidate(
        CandidateProfile candidate,
        float skillScore,
        float experienceScore,
        float interestScore,
        float totalScore
) {
}
