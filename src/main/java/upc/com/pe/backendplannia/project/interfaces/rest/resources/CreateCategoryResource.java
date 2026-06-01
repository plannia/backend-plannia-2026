package upc.com.pe.backendplannia.project.interfaces.rest.resources;

import java.time.LocalDateTime;

public record CreateCategoryResource(Long teamId, String name, LocalDateTime limitDate) {
    public CreateCategoryResource {
        if (teamId == null || name == null || name.isBlank() || limitDate == null) {
            throw new IllegalArgumentException("All fields are required");
        }
    }
}
