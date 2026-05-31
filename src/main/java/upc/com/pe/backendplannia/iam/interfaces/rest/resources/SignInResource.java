package upc.com.pe.backendplannia.iam.interfaces.rest.resources;

public record SignInResource(String email, String password) {
    public SignInResource {
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("All fields are required");
        }
    }
}
