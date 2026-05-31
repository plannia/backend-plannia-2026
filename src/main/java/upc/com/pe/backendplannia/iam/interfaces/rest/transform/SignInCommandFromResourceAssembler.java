package upc.com.pe.backendplannia.iam.interfaces.rest.transform;

import upc.com.pe.backendplannia.iam.domain.model.commands.SignInCommand;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.SignInResource;

public class SignInCommandFromResourceAssembler {
    public static SignInCommand toCommandFromResource(SignInResource resource) {
        return new SignInCommand(resource.email(), resource.password());
    }
}
