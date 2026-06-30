package upc.com.pe.backendplannia.project.domain.model.queries;

public record GetTasksForPlannerQuery(Long teamId) {
    public GetTasksForPlannerQuery {
        if (teamId == null || teamId <= 0) {
            throw new IllegalArgumentException("teamId must be a positive number");
        }
    }
}
