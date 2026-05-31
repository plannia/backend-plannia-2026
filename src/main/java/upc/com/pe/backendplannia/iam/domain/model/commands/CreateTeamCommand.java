package upc.com.pe.backendplannia.iam.domain.model.commands;

public record CreateTeamCommand(String teamName, String email, String leaderName, String password) {
    public CreateTeamCommand {
        if (teamName.isBlank() || email.isBlank() || leaderName.isBlank() || password.isBlank())
            throw new IllegalArgumentException("All fields are required");
    }
}
