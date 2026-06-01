package upc.com.pe.backendplannia.shared.domain.model.events;

public record TaskMarkedAsDoneEvent(Long taskId, Long userId) {
    public TaskMarkedAsDoneEvent {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
    }
}
