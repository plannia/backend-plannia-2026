package upc.com.pe.backendplannia.iam.interfaces.rest.transform;

import upc.com.pe.backendplannia.iam.domain.model.aggregates.User;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.UserResource;

public class UserResourceFromEntityAssembler {
    public static UserResource toResourceFromEntity(User user) {
        return new UserResource(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPosition(),
                user.getRole()
        );
    }
}
