package upc.com.pe.backendplannia.project.domain.model.commands;

public record MarkTaskAsAssignedCommand(Long taskId) {
    public MarkTaskAsAssignedCommand {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId cannot be null");
        }
    }
}
