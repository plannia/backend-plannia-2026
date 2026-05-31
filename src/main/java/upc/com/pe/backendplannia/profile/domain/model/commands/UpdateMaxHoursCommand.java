package upc.com.pe.backendplannia.profile.domain.model.commands;

public record UpdateMaxHoursCommand(
        Long userId,
        float maxHours
) {
}
