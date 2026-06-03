package upc.com.pe.backendplannia.notifications.interfaces.rest.resources;

public record NotificationResource(
        Long id,
        Long userId,
        Long taskId,
        String email,
        String message,
        String channel
) {
}
