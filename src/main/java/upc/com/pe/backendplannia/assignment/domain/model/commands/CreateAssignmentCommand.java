package upc.com.pe.backendplannia.assignment.domain.model.commands;

public record CreateAssignmentCommand(
        Long userId,
        Long taskId,
        float skillMatch,
        float experienceMatch,
        float interestMatch,
        float score
) {
}
