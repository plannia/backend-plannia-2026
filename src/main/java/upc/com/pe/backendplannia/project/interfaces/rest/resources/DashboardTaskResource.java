package upc.com.pe.backendplannia.project.interfaces.rest.resources;

import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;

import java.time.LocalDateTime;

public record DashboardTaskResource(
        Long userId,
        String userName,
        Long taskId,
        String taskName,
        Status taskStatus,
        LocalDateTime taskStartTime,
        LocalDateTime taskEndTime,
        Integer taskHours,
        Long taskCategoryId,
        String taskCategoryName
) {
}
