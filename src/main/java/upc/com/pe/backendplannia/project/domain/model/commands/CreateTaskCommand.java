package upc.com.pe.backendplannia.project.domain.model.commands;

import java.time.LocalDateTime;
import java.util.List;

public record CreateTaskCommand(
        Long categoryId,
        String title,
        String description,
        Integer hours,
        String priority,
        String difficulty,
        LocalDateTime limitDate,
        List<String> tools,
        List<String> knowledge
) {
    public CreateTaskCommand {
        if (categoryId == null || title == null || title.isBlank() || description == null || description.isBlank()
                || hours == null || priority == null || priority.isBlank()
                || difficulty == null || difficulty.isBlank() || limitDate == null) {
            throw new IllegalArgumentException("All fields are required");
        }
        if (hours <= 0) throw new IllegalArgumentException("Hours must be greater than zero");
    }
}
