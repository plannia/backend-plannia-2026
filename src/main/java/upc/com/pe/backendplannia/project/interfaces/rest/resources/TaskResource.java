package upc.com.pe.backendplannia.project.interfaces.rest.resources;

import upc.com.pe.backendplannia.project.domain.model.valueobjects.Difficulty;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Priority;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;

import java.time.LocalDateTime;
import java.util.List;

public record TaskResource(
        Long id,
        Long categoryId,
        String title,
        String description,
        Integer hours,
        Priority priority,
        Difficulty difficulty,
        Status status,
        LocalDateTime limitDate,
        List<String> tools,
        List<String> knowledge,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
