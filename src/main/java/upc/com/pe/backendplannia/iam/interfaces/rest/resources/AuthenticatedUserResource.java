package upc.com.pe.backendplannia.iam.interfaces.rest.resources;

public record AuthenticatedUserResource(
        UserResource user,
        String token
) {
}
