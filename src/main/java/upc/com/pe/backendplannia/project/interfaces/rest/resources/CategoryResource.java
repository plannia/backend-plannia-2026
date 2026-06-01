package upc.com.pe.backendplannia.project.interfaces.rest.resources;

import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;

import java.time.LocalDateTime;
import java.util.List;

public record CategoryResource(
        Long id,
        Long teamId,
        String name,
        LocalDateTime limitDate,
        Status status,
        List<Long> memberIds
) {
}
