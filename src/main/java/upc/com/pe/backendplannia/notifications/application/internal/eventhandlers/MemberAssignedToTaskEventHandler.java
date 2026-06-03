package upc.com.pe.backendplannia.notifications.application.internal.eventhandlers;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import upc.com.pe.backendplannia.notifications.domain.model.commands.SendAssignmentNotificationCommand;
import upc.com.pe.backendplannia.notifications.domain.services.NotificationCommandService;
import upc.com.pe.backendplannia.shared.domain.model.events.MemberAssignedToTaskEvent;

/**
 * Escucha "se asignó a un miembro a una tarea" y dispara el correo.
 *
 * Por qué NO un @EventListener normal: enviar correo es I/O externo. Con:
 *  - {@code @TransactionalEventListener(AFTER_COMMIT)} solo notificamos si la asignación SÍ se guardó
 *    (si la transacción de Assignment hace rollback, este handler ni se ejecuta);
 *  - {@code @Async} el SMTP corre en otro hilo: su latencia o fallo no bloquea ni revierte la asignación.
 *
 * Requiere {@code @EnableAsync} en la clase principal (ver BackendPlanniaApplication).
 */
@Component
public class MemberAssignedToTaskEventHandler {
    private final NotificationCommandService notificationCommandService;

    public MemberAssignedToTaskEventHandler(NotificationCommandService notificationCommandService) {
        this.notificationCommandService = notificationCommandService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MemberAssignedToTaskEvent event) {
        notificationCommandService.handle(
                new SendAssignmentNotificationCommand(event.userId(), event.taskId()));
    }
}
