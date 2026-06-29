package upc.com.pe.backendplannia.assignment.interfaces.rest.resources;

import java.util.List;

/**
 * Resultado del auto-assign: las asignaciones creadas y los ids de las tareas que quedaron sin asignar
 * por no haber candidato disponible.
 */
public record AutoAssignResultResource(
        List<AssignmentResource> assigned,
        List<Long> skippedTaskIds
) {
}
