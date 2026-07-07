package upc.com.pe.backendplannia.assignment.interfaces.rest.resources;

public record CandidateProfileResource(
        Long userId,
        float activeHours,
        float maxHours,
        float availableHours,
        // Desglose del match contra la tarea (0..1). experienceScore ya viene post-piso de relevancia.
        float skillScore,
        float experienceScore,
        float interestScore,
        float totalScore
) {
}
