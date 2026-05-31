package upc.com.pe.backendplannia.iam.domain.model.commands;

public record SignUpCommand(String name, String email, String password, String position, String code) {
    public SignUpCommand {
        if (name.isBlank() || email.isBlank() || password.isBlank() || position.isBlank() || code.isBlank())
            throw new IllegalArgumentException("All fields are required");
    }
}
