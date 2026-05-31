package upc.com.pe.backendplannia.iam.interfaces.rest.transform;

import upc.com.pe.backendplannia.iam.domain.model.aggregates.User;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.AuthenticatedUserResource;

public class AuthenticatedUserResourceFromEntityAssembler {
    public static AuthenticatedUserResource toResourceFromEntityAndToken(User user, String token) {
        return new AuthenticatedUserResource(
                UserResourceFromEntityAssembler.toResourceFromEntity(user),
                token
        );
    }
}
