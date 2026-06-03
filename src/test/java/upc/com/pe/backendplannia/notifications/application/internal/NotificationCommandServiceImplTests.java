package upc.com.pe.backendplannia.notifications.application.internal;

import org.junit.jupiter.api.Test;
import upc.com.pe.backendplannia.notifications.application.internal.commandservices.NotificationCommandServiceImpl;
import upc.com.pe.backendplannia.notifications.domain.model.aggregates.Notification;
import upc.com.pe.backendplannia.notifications.domain.model.commands.SendAssignmentNotificationCommand;
import upc.com.pe.backendplannia.notifications.domain.model.valueobjects.UserContactSnapshot;
import upc.com.pe.backendplannia.notifications.domain.services.EmailSender;
import upc.com.pe.backendplannia.notifications.domain.services.UserContactPort;
import upc.com.pe.backendplannia.notifications.infrastructure.persistence.jpa.repositories.NotificationRepository;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationCommandServiceImplTests {
    private static final Long USER_ID = 101L;
    private static final Long TASK_ID = 501L;
    private static final String EMAIL = "member@plannia.com";
    private static final int MAX_PER_DAY = 90;

    private final UserContactPort userContactPort = mock(UserContactPort.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final NotificationCommandServiceImpl service =
            new NotificationCommandServiceImpl(userContactPort, emailSender, notificationRepository, MAX_PER_DAY);

    private void givenTeamMember() {
        when(userContactPort.findByUserId(USER_ID))
                .thenReturn(Optional.of(new UserContactSnapshot(USER_ID, "Ana", EMAIL, true)));
    }

    @Test
    void sendsEmailAndPersistsNotificationForTeamMember() {
        givenTeamMember();
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.handle(new SendAssignmentNotificationCommand(USER_ID, TASK_ID));

        assertThat(result).isPresent();
        verify(notificationRepository).save(any(Notification.class));
        // El correo va dirigido al email del miembro y menciona el id de la tarea.
        verify(emailSender).send(eq(EMAIL), anyString(), eq("Hola Ana, se te asignó la tarea #" + TASK_ID + "."));
    }

    @Test
    void doesNothingWhenUserIsNotTeamMember() {
        when(userContactPort.findByUserId(USER_ID))
                .thenReturn(Optional.of(new UserContactSnapshot(USER_ID, "Leo", EMAIL, false)));

        var result = service.handle(new SendAssignmentNotificationCommand(USER_ID, TASK_ID));

        assertThat(result).isEmpty();
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void doesNothingWhenUserNotFound() {
        when(userContactPort.findByUserId(USER_ID)).thenReturn(Optional.empty());

        var result = service.handle(new SendAssignmentNotificationCommand(USER_ID, TASK_ID));

        assertThat(result).isEmpty();
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void doesNotResendForSameUserAndTaskWithinTheDay() {
        givenTeamMember();
        // Ya existe una notificación de hoy para este usuario+tarea (anti-loop).
        when(notificationRepository.existsByUserIdAndTaskIdAndCreatedAtGreaterThanEqual(
                eq(USER_ID), eq(TASK_ID), any(Date.class))).thenReturn(true);

        var result = service.handle(new SendAssignmentNotificationCommand(USER_ID, TASK_ID));

        assertThat(result).isEmpty();
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void doesNotSendWhenDailyCapReached() {
        givenTeamMember();
        // Ya se alcanzó el tope diario: no hay duplicado, pero el contador global está al límite.
        when(notificationRepository.countByCreatedAtGreaterThanEqual(any(Date.class)))
                .thenReturn((long) MAX_PER_DAY);

        var result = service.handle(new SendAssignmentNotificationCommand(USER_ID, TASK_ID));

        assertThat(result).isEmpty();
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }
}
