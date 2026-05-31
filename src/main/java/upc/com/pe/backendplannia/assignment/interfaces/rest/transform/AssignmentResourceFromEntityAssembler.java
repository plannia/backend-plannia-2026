package upc.com.pe.backendplannia.assignment.interfaces.rest.transform;

import upc.com.pe.backendplannia.assignment.domain.model.aggregates.Assignment;
import upc.com.pe.backendplannia.assignment.interfaces.rest.resources.AssignmentResource;

public class AssignmentResourceFromEntityAssembler {
    public static AssignmentResource toResourceFromEntity(Assignment assignment) {
        return new AssignmentResource(
                assignment.getId(),
                assignment.getUserId(),
                assignment.getTaskId(),
                assignment.getSkillMatch(),
                assignment.getExperienceMatch(),
                assignment.getInterestMatch(),
                assignment.getScore(),
                assignment.getStatus().name()
        );
    }
}
