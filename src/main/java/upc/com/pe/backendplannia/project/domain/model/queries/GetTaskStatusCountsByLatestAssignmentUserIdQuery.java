package upc.com.pe.backendplannia.project.domain.model.queries;

public record GetTaskStatusCountsByLatestAssignmentUserIdQuery(Long userId) {
    public GetTaskStatusCountsByLatestAssignmentUserIdQuery {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
    }
}
