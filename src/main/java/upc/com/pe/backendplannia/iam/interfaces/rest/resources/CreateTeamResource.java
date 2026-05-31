package upc.com.pe.backendplannia.iam.interfaces.rest.resources;

public record CreateTeamResource(String teamName, String email, String leaderName, String password) {
    public CreateTeamResource {
        if (teamName.isBlank() || email.isBlank() || leaderName.isBlank() || password.isBlank())
            throw new IllegalArgumentException("All fields are required");
    }
}