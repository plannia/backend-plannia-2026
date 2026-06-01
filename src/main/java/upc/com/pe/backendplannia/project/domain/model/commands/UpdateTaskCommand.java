package upc.com.pe.backendplannia.project.domain.model.commands;

import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;

import java.time.LocalDateTime;

public record UpdateTaskCommand(Long id, String status, LocalDateTime limitDate) {
    public UpdateTaskCommand {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        try {
            Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid status");
        }
    }
}
