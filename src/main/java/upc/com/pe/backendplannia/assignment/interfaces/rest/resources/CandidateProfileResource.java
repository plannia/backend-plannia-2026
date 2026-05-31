package upc.com.pe.backendplannia.assignment.interfaces.rest.resources;

public record CandidateProfileResource(
        Long userId,
        float activeHours,
        float maxHours,
        float availableHours
) {
}
