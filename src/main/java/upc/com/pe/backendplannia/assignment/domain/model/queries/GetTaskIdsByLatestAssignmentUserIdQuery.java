package upc.com.pe.backendplannia.assignment.domain.model.queries;

public record GetTaskIdsByLatestAssignmentUserIdQuery(Long userId) {
    public GetTaskIdsByLatestAssignmentUserIdQuery {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
    }
}
