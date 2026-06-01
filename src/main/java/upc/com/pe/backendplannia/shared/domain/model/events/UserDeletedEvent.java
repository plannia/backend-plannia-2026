package upc.com.pe.backendplannia.shared.domain.model.events;

public record UserDeletedEvent(Long userId) {
    public UserDeletedEvent {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
    }
}
