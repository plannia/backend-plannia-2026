package upc.com.pe.backendplannia.project.interfaces.rest.transform;

import upc.com.pe.backendplannia.project.domain.model.commands.CreateCategoryCommand;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.CreateCategoryResource;

public class CreateCategoryCommandFromResourceAssembler {
    public static CreateCategoryCommand toCommandFromResource(CreateCategoryResource resource) {
        return new CreateCategoryCommand(
                resource.teamId(),
                resource.name(),
                resource.limitDate()
        );
    }
}
