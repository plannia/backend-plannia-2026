package upc.com.pe.backendplannia.shared.domain.model.events;

import java.util.List;

public record UserAssignmentsDeactivatedEvent(List<Long> taskIds) {
    public UserAssignmentsDeactivatedEvent {
        if (taskIds == null) {
            throw new IllegalArgumentException("taskIds cannot be null");
        }
    }
}
