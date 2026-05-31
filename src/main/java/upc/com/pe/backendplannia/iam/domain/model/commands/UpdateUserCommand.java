package upc.com.pe.backendplannia.iam.domain.model.commands;

public record UpdateUserCommand(Long id, String position, String name, String email, String password) {
    public UpdateUserCommand {
        if (id == null) throw new IllegalArgumentException("id cannot be null");
    }
}
