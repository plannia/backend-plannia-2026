package upc.com.pe.backendplannia.project.domain.model.queries;

public record GetTasksByFilterQuery(
        Long teamId,
        String title,
        String description,
        String priority,
        String difficulty,
        String status,
        String categoryId,
        String userId
) {
    public GetTasksByFilterQuery {
        if (teamId == null) {
            throw new IllegalArgumentException("teamId is required");
        }
    }
}
