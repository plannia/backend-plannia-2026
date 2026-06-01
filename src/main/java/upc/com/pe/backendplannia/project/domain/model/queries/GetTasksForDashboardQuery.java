package upc.com.pe.backendplannia.project.domain.model.queries;

public record GetTasksForDashboardQuery(Long teamId) {
    public GetTasksForDashboardQuery {
        if (teamId == null) {
            throw new IllegalArgumentException("teamId is required");
        }
    }
}
