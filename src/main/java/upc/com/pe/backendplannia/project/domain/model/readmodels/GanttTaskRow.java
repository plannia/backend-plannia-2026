package upc.com.pe.backendplannia.project.domain.model.readmodels;

import java.time.LocalDate;

/**
 * Una fila del Gantt. {@code estimated} = true cuando la tarea aún no arrancó
 * (TO_DO) y sus fechas se estimaron a partir de la fecha límite: se dibuja atenuada.
 */
public record GanttTaskRow(
        Long taskId,
        String title,
        Long assigneeUserId,
        String assigneeName,
        String statusLabel,
        String priorityLabel,
        String difficultyLabel,
        Integer hours,
        String progressLabel,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate dueDate,
        boolean estimated
) {
}
