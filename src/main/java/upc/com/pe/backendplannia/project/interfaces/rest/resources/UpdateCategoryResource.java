package upc.com.pe.backendplannia.project.interfaces.rest.resources;

import java.time.LocalDateTime;

public record UpdateCategoryResource(String name, String status, LocalDateTime limitDate) {
}
