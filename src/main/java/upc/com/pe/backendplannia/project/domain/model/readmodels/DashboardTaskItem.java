package upc.com.pe.backendplannia.project.domain.model.readmodels;

import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;

import java.time.LocalDateTime;

public record DashboardTaskItem(
        Long userId,
        String userName,
        Long taskId,
        String taskName,
        Status taskStatus,
        LocalDateTime taskStartTime,
        Integer taskHours,
        Long taskCategoryId,
        String taskCategoryName
) {
}
