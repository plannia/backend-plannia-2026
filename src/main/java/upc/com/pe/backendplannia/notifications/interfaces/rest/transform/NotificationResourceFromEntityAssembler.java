package upc.com.pe.backendplannia.notifications.interfaces.rest.transform;

import upc.com.pe.backendplannia.notifications.domain.model.aggregates.Notification;
import upc.com.pe.backendplannia.notifications.interfaces.rest.resources.NotificationResource;

public class NotificationResourceFromEntityAssembler {
    public static NotificationResource toResourceFromEntity(Notification notification) {
        return new NotificationResource(
                notification.getId(),
                notification.getUserId(),
                notification.getTaskId(),
                notification.getEmail(),
                notification.getMessage(),
                notification.getChannel()
        );
    }
}
