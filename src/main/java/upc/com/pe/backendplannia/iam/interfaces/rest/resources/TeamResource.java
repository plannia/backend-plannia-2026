package upc.com.pe.backendplannia.iam.interfaces.rest.resources;

import java.util.List;

public record TeamResource(
        Long id,
        String name,
        String code,
        List<UserResource> members
) {
}
