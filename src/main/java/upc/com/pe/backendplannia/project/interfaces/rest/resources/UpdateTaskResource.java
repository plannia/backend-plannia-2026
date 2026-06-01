package upc.com.pe.backendplannia.project.interfaces.rest.resources;

import java.time.LocalDateTime;

public record UpdateTaskResource(String status, LocalDateTime limitDate) {
    public UpdateTaskResource {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
    }
}
