package upc.com.pe.backendplannia.project.domain.model.commands;

import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;

import java.time.LocalDateTime;

public record UpdateCategoryCommand(Long id, String name, String status, LocalDateTime limitDate) {
    public UpdateCategoryCommand {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (status != null && !status.isBlank()
                && !Status.CANCELLED.name().equalsIgnoreCase(status.trim())) {
            throw new IllegalArgumentException("Only CANCELLED status can be set manually");
        }
    }
}
