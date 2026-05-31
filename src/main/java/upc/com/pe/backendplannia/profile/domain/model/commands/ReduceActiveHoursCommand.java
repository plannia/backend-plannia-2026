package upc.com.pe.backendplannia.profile.domain.model.commands;

public record ReduceActiveHoursCommand(
        Long userId,
        float hours
) {
}
