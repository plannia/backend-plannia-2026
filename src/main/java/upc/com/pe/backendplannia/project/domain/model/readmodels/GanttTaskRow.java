package upc.com.pe.backendplannia.project.domain.model.readmodels;

import java.time.LocalDate;

public record GanttTaskRow(
        Long taskId,
        String title,
        Long assigneeUserId,
        String assigneeName,
        String progressLabel,
        LocalDate startDate,
        LocalDate endDate
) {
}
