package upc.com.pe.backendplannia.assignment.domain.model.commands;

public record DeactivateUserAssignmentsCommand(Long userId) {
    public DeactivateUserAssignmentsCommand {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
    }
}
