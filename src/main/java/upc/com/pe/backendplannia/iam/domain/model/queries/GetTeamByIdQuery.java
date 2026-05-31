package upc.com.pe.backendplannia.iam.domain.model.queries;

public record GetTeamByIdQuery(Long teamId) {
    public GetTeamByIdQuery {
        if (teamId == null) throw new IllegalArgumentException("teamId cannot be null");
    }
}
