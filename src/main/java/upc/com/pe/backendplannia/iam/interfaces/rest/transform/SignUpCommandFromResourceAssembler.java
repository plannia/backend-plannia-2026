package upc.com.pe.backendplannia.iam.interfaces.rest.transform;

import upc.com.pe.backendplannia.iam.domain.model.commands.SignUpCommand;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.SignUpResource;

public class SignUpCommandFromResourceAssembler {
    public static SignUpCommand toCommandFromResource(SignUpResource resource) {
        return new SignUpCommand(
                resource.name(),
                resource.email(),
                resource.password(),
                resource.position(),
                resource.code()
        );
    }
}
