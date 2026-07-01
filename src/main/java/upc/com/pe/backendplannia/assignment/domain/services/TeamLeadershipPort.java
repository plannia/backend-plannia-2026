package upc.com.pe.backendplannia.assignment.domain.services;

import java.util.Optional;

/**
 * Puerto ACL hacia IAM: resuelve quién es el LÍDER de un equipo.
 *
 * <p>Hoy se usa para EXCLUIR al líder del pool de candidatos (recomendación manual y auto-assign):
 * el líder organiza, no se le reparten tareas automáticamente. La pieza queda aislada acá a propósito
 * para la integración futura, donde el filtro pasará a ser opcional (p. ej. un flag {@code includeLeader}
 * en el auto-assign y mostrar el score comparado del líder en la recomendación).
 */
public interface TeamLeadershipPort {
    Optional<Long> findLeaderUserId(Long teamId);
}
