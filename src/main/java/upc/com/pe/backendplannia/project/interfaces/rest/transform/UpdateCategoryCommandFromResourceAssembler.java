package upc.com.pe.backendplannia.project.interfaces.rest.transform;

import upc.com.pe.backendplannia.project.domain.model.commands.UpdateCategoryCommand;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.UpdateCategoryResource;

public class UpdateCategoryCommandFromResourceAssembler {
    public static UpdateCategoryCommand toCommandFromResource(Long categoryId, UpdateCategoryResource resource) {
        return new UpdateCategoryCommand(
                categoryId,
                resource.name(),
                resource.status(),
                resource.limitDate()
        );
    }
}
