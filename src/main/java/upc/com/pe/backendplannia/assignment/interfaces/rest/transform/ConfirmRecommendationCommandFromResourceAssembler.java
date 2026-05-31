package upc.com.pe.backendplannia.assignment.interfaces.rest.transform;

import upc.com.pe.backendplannia.assignment.domain.model.commands.ConfirmRecommendationCommand;
import upc.com.pe.backendplannia.assignment.interfaces.rest.resources.ConfirmRecommendationResource;

public class ConfirmRecommendationCommandFromResourceAssembler {
    public static ConfirmRecommendationCommand toCommandFromResource(ConfirmRecommendationResource resource) {
        return new ConfirmRecommendationCommand(resource.taskId(), resource.userId());
    }
}
