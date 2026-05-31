package upc.com.pe.backendplannia.assignment.interfaces.rest.resources;

public record AssignmentResource(
        Long id,
        Long userId,
        Long taskId,
        float skillMatch,
        float experienceMatch,
        float interestMatch,
        float score,
        String status
) {
}
