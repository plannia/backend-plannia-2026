package upc.com.pe.backendplannia.notifications.domain.services;

import upc.com.pe.backendplannia.notifications.domain.model.aggregates.Notification;
import upc.com.pe.backendplannia.notifications.domain.model.commands.SendAssignmentNotificationCommand;

import java.util.Optional;

public interface NotificationCommandService {
    // Vacío si no hay nada que notificar (usuario inexistente o no es team member).
    Optional<Notification> handle(SendAssignmentNotificationCommand command);
}
