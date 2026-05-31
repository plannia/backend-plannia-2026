package upc.com.pe.backendplannia.assignment.domain.model.commands;

public record AutoAssignProjectCommand(
        Long teamId,
        Long projectId
) {
}
