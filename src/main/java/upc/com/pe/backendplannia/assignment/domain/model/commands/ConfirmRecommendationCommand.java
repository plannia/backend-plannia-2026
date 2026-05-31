package upc.com.pe.backendplannia.assignment.domain.model.commands;

public record ConfirmRecommendationCommand(
        Long taskId,
        Long userId
) {
}
