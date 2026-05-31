package upc.com.pe.backendplannia.assignment.interfaces.rest.resources;

public record ConfirmRecommendationResource(
        Long taskId,
        Long userId
) {
    public ConfirmRecommendationResource {
        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException("Task id must be a positive number");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User id must be a positive number");
        }
    }
}
