package upc.com.pe.backendplannia.assignment.domain.model.queries;

public record GetLatestAssignmentByTaskIdQuery(Long taskId) {
    public GetLatestAssignmentByTaskIdQuery {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId cannot be null");
        }
    }
}
