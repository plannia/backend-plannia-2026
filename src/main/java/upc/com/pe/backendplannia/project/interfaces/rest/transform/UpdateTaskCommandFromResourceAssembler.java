package upc.com.pe.backendplannia.project.interfaces.rest.transform;

import upc.com.pe.backendplannia.project.domain.model.commands.UpdateTaskCommand;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.UpdateTaskResource;

public class UpdateTaskCommandFromResourceAssembler {
    public static UpdateTaskCommand toCommandFromResource(Long taskId, UpdateTaskResource resource) {
        return new UpdateTaskCommand(taskId, resource.status(), resource.limitDate());
    }
}
