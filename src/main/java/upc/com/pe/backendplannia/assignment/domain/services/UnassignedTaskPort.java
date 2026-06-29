package upc.com.pe.backendplannia.assignment.domain.services;

import upc.com.pe.backendplannia.assignment.domain.model.readmodels.BacklogTask;

import java.util.List;

/**
 * Puerto (ACL) hacia Project para obtener el backlog de tareas ASIGNABLES de un scope.
 * Asignable = sin asignar y no terminada. Lo implementa un adapter en infrastructure que conoce a Project.
 */
public interface UnassignedTaskPort {
    List<BacklogTask> findUnassignedByTeamId(Long teamId);

    List<BacklogTask> findUnassignedByCategoryId(Long categoryId);
}
