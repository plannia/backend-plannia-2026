package upc.com.pe.backendplannia.project.interfaces.rest.transform;

import upc.com.pe.backendplannia.project.domain.model.readmodels.DashboardTaskItem;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.DashboardTaskResource;

public class DashboardTaskResourceFromReadModelAssembler {
    public static DashboardTaskResource toResourceFromReadModel(DashboardTaskItem item) {
        return new DashboardTaskResource(
                item.userId(),
                item.userName(),
                item.taskId(),
                item.taskName(),
                item.taskStatus(),
                item.taskStartTime(),
                item.taskEndTime(),
                item.taskHours(),
                item.taskCategoryId(),
                item.taskCategoryName()
        );
    }
}
