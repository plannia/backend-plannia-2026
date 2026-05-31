package upc.com.pe.backendplannia.iam.interfaces.rest.resources;

public record SignUpResource(String name, String email, String password, String position, String code) {
    public SignUpResource {
        if (name.isBlank() || email.isBlank() || password.isBlank() || position.isBlank() || code.isBlank()) {
            throw new IllegalArgumentException("All fields are required");
        }
    }
}
