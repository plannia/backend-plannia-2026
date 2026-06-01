package upc.com.pe.backendplannia.project.domain.model.queries;

public record GetAllCategoriesQuery(Long teamId) {
    public GetAllCategoriesQuery {
        if (teamId == null) {
            throw new IllegalArgumentException("teamId is required");
        }
    }
}