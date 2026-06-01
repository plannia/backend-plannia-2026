package upc.com.pe.backendplannia.project.domain.services;

import upc.com.pe.backendplannia.project.domain.model.aggregates.Task;
import upc.com.pe.backendplannia.project.domain.model.queries.GetTasksByFilterQuery;
import upc.com.pe.backendplannia.project.domain.model.queries.GetTasksForDashboardQuery;
import upc.com.pe.backendplannia.project.domain.model.queries.GetTaskStatusCountsByLatestAssignmentUserIdQuery;
import upc.com.pe.backendplannia.project.domain.model.readmodels.DashboardTaskItem;
import upc.com.pe.backendplannia.project.domain.model.readmodels.TaskStatusCounts;

import java.util.List;

public interface TaskQueryService {
    List<Task> handle(GetTasksByFilterQuery query);

    List<DashboardTaskItem> handle(GetTasksForDashboardQuery query);

    TaskStatusCounts handle(GetTaskStatusCountsByLatestAssignmentUserIdQuery query);
}
