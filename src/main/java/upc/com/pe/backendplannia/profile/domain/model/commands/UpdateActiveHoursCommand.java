package upc.com.pe.backendplannia.profile.domain.model.commands;

public record UpdateActiveHoursCommand(
        Long userId,
        float hours
) {
}
