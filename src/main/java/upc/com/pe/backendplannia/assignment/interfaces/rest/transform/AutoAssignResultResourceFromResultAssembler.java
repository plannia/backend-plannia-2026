package upc.com.pe.backendplannia.assignment.interfaces.rest.transform;

import upc.com.pe.backendplannia.assignment.domain.model.readmodels.AutoAssignmentResult;
import upc.com.pe.backendplannia.assignment.interfaces.rest.resources.AutoAssignResultResource;

public class AutoAssignResultResourceFromResultAssembler {
    public static AutoAssignResultResource toResourceFromResult(AutoAssignmentResult result) {
        var assigned = result.assignments().stream()
                .map(AssignmentResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return new AutoAssignResultResource(assigned, result.skippedTaskIds());
    }
}
