package upc.com.pe.backendplannia.iam.domain.model.commands;

public record DeleteUserCommand(Long userId) {
    public DeleteUserCommand {
        if (userId == null) throw new IllegalArgumentException("userId cannot be null");
    }
}
