package upc.com.pe.backendplannia.assignment.domain.services;

import upc.com.pe.backendplannia.assignment.domain.model.readmodels.TaskRequirement;

import java.util.Optional;

public interface TaskRequirementResolver {
    Optional<TaskRequirement> resolveByTaskId(Long taskId);
}
