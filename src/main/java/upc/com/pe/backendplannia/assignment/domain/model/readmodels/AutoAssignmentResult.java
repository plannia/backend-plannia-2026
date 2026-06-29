package upc.com.pe.backendplannia.assignment.domain.model.readmodels;

import upc.com.pe.backendplannia.assignment.domain.model.aggregates.Assignment;

import java.util.List;

/**
 * Resultado de una corrida de auto-assign: las asignaciones creadas y las tareas que se saltaron por
 * no tener ningún candidato disponible (todos sin horas, o sin skills/embedding, o que no se pudieron
 * puntuar).
 */
public record AutoAssignmentResult(
        List<Assignment> assignments,
        List<Long> skippedTaskIds
) {
}
