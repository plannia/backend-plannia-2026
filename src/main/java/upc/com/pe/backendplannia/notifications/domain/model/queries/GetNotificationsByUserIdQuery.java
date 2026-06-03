package upc.com.pe.backendplannia.notifications.domain.model.queries;

public record GetNotificationsByUserIdQuery(Long userId) {
    public GetNotificationsByUserIdQuery {
        if (userId == null) throw new IllegalArgumentException("userId cannot be null");
    }
}
