package upc.com.pe.backendplannia.notifications.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import upc.com.pe.backendplannia.notifications.domain.model.aggregates.Notification;

import java.util.Date;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findAllByOrderByCreatedAtDesc();

    // Anti-duplicado (anti-loop): ¿ya se notificó a este usuario por esta tarea desde 'since'?
    boolean existsByUserIdAndTaskIdAndCreatedAtGreaterThanEqual(Long userId, Long taskId, Date since);

    // Tope diario: cuántos correos se han registrado desde 'since' (inicio del día).
    long countByCreatedAtGreaterThanEqual(Date since);
}
