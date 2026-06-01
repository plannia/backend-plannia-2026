package upc.com.pe.backendplannia.project.domain.model.commands;

import java.time.LocalDateTime;

public record CreateCategoryCommand(Long teamId, String name, LocalDateTime limitDate) {
    public CreateCategoryCommand {
        if (teamId == null || name == null || name.isBlank() || limitDate == null)
            throw new IllegalArgumentException("All fields are required");
    }
}
