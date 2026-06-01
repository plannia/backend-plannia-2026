package upc.com.pe.backendplannia.project.interfaces.rest.transform;

import upc.com.pe.backendplannia.project.domain.model.commands.AddCategoryMemberCommand;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.AddCategoryMemberResource;

public class AddCategoryMemberCommandFromResourceAssembler {
    public static AddCategoryMemberCommand toCommandFromResource(Long categoryId, AddCategoryMemberResource resource) {
        return new AddCategoryMemberCommand(categoryId, resource.userId());
    }
}
