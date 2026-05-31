package upc.com.pe.backendplannia.iam.domain.model.commands;

public record UpdateUserCommand(String position, String name, String email, String password) {
    public UpdateUserCommand {
    }
}
