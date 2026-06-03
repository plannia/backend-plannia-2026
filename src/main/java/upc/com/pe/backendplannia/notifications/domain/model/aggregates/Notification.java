package upc.com.pe.backendplannia.notifications.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import upc.com.pe.backendplannia.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;

// Registro/outbox de cada notificación que se intentó enviar. Sirve de historial y para depurar.
@Entity
@Getter
public class Notification extends AuditableAbstractAggregateRoot<Notification> {
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private String channel;

    protected Notification() {
    }

    public Notification(Long userId, Long taskId, String email, String message, String channel) {
        this.userId = userId;
        this.taskId = taskId;
        this.email = email;
        this.message = message;
        this.channel = channel;
    }
}
