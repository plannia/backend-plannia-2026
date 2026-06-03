package upc.com.pe.backendplannia.notifications.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.notifications.domain.model.aggregates.Notification;
import upc.com.pe.backendplannia.notifications.domain.model.queries.GetAllNotificationsQuery;
import upc.com.pe.backendplannia.notifications.domain.model.queries.GetNotificationsByUserIdQuery;
import upc.com.pe.backendplannia.notifications.domain.services.NotificationQueryService;
import upc.com.pe.backendplannia.notifications.infrastructure.persistence.jpa.repositories.NotificationRepository;

import java.util.List;

@Service
public class NotificationQueryServiceImpl implements NotificationQueryService {
    private final NotificationRepository notificationRepository;

    public NotificationQueryServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> handle(GetAllNotificationsQuery query) {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> handle(GetNotificationsByUserIdQuery query) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(query.userId());
    }
}
