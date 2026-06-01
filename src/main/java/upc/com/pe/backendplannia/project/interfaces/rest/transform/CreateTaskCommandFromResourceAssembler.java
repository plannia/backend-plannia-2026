package upc.com.pe.backendplannia.project.interfaces.rest.transform;

import upc.com.pe.backendplannia.project.domain.model.commands.CreateTaskCommand;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.CreateTaskResource;

public class CreateTaskCommandFromResourceAssembler {
    public static CreateTaskCommand toCommandFromResource(CreateTaskResource resource) {
        return new CreateTaskCommand(
                resource.categoryId(),
                resource.title(),
                resource.description(),
                resource.hours(),
                resource.priority(),
                resource.difficulty(),
                resource.limitDate(),
                resource.tools(),
                resource.knowledge()
        );
    }
}
