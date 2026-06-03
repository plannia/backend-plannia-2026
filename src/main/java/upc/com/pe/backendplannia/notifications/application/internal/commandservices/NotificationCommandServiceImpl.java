package upc.com.pe.backendplannia.notifications.application.internal.commandservices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.notifications.domain.model.aggregates.Notification;
import upc.com.pe.backendplannia.notifications.domain.model.commands.SendAssignmentNotificationCommand;
import upc.com.pe.backendplannia.notifications.domain.services.EmailSender;
import upc.com.pe.backendplannia.notifications.domain.services.NotificationCommandService;
import upc.com.pe.backendplannia.notifications.domain.services.UserContactPort;
import upc.com.pe.backendplannia.notifications.infrastructure.persistence.jpa.repositories.NotificationRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

@Service
public class NotificationCommandServiceImpl implements NotificationCommandService {
    private static final Logger log = LoggerFactory.getLogger(NotificationCommandServiceImpl.class);
    private static final String CHANNEL_EMAIL = "EMAIL";

    private final UserContactPort userContactPort;
    private final EmailSender emailSender;
    private final NotificationRepository notificationRepository;
    private final int maxPerDay;

    public NotificationCommandServiceImpl(
            UserContactPort userContactPort,
            EmailSender emailSender,
            NotificationRepository notificationRepository,
            @Value("${notifications.email.max-per-day:90}") int maxPerDay
    ) {
        this.userContactPort = userContactPort;
        this.emailSender = emailSender;
        this.notificationRepository = notificationRepository;
        this.maxPerDay = maxPerDay;
    }

    @Override
    @Transactional
    public Optional<Notification> handle(SendAssignmentNotificationCommand command) {
        var contact = userContactPort.findByUserId(command.userId());

        // Solo notificamos a team members; líderes (o usuarios inexistentes) se ignoran sin error.
        if (contact.isEmpty() || !contact.get().teamMember()) {
            return Optional.empty();
        }

        var startOfToday = startOfToday();

        // GUARDA 1 (anti-loop): no notificar dos veces al mismo usuario por la misma tarea en el día.
        // Mata cualquier re-disparo del evento o reasignación repetida sin gastar cuota.
        if (notificationRepository.existsByUserIdAndTaskIdAndCreatedAtGreaterThanEqual(
                command.userId(), command.taskId(), startOfToday)) {
            log.debug("Notificación duplicada omitida (userId={}, taskId={})", command.userId(), command.taskId());
            return Optional.empty();
        }

        // GUARDA 2 (tope diario): freno global para no agotar la cuota del proveedor (Resend free = 100/día).
        long sentToday = notificationRepository.countByCreatedAtGreaterThanEqual(startOfToday);
        if (sentToday >= maxPerDay) {
            log.warn("Tope diario de correos alcanzado ({}). Se omite la notificación a userId={}.",
                    maxPerDay, command.userId());
            return Optional.empty();
        }

        var to = contact.get();
        var subject = "Nueva tarea asignada";
        var message = "Hola " + to.name() + ", se te asignó la tarea #" + command.taskId() + ".";

        var savedNotification = notificationRepository.save(
                new Notification(to.userId(), command.taskId(), to.email(), message, CHANNEL_EMAIL));
        emailSender.send(to.email(), subject, message);

        return Optional.of(savedNotification);
    }

    private static Date startOfToday() {
        return Date.from(LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
