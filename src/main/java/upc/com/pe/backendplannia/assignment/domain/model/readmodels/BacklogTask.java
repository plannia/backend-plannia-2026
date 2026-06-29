package upc.com.pe.backendplannia.assignment.domain.model.readmodels;

import java.time.LocalDateTime;

/**
 * Tarea pendiente de asignar dentro de un scope (equipo o categoría), con lo MÍNIMO para ordenar el
 * backlog del auto-assign. No trae embedding a propósito: ordenar es barato y no debe gastar IA; el
 * embedding se resuelve después, ya en orden, vía {@link TaskRequirement}.
 *
 * <p>{@code priorityRank} y {@code difficultyRank} son números mayores = más urgente / más difícil
 * (el adapter ACL de Project traduce sus enums a estos rangos, para no acoplar Assignment a Project).
 */
public record BacklogTask(
        Long taskId,
        int priorityRank,
        int difficultyRank,
        LocalDateTime limitDate
) {
}
