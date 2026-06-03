package upc.com.pe.backendplannia.notifications.domain.services;

import upc.com.pe.backendplannia.notifications.domain.model.aggregates.Notification;
import upc.com.pe.backendplannia.notifications.domain.model.queries.GetAllNotificationsQuery;
import upc.com.pe.backendplannia.notifications.domain.model.queries.GetNotificationsByUserIdQuery;

import java.util.List;

public interface NotificationQueryService {
    List<Notification> handle(GetAllNotificationsQuery query);

    List<Notification> handle(GetNotificationsByUserIdQuery query);
}
