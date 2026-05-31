package upc.com.pe.backendplannia.iam.domain.model.commands;

public record SignInCommand(String email, String password) {
    public SignInCommand {
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("All fields are required");
        }
    }
}
