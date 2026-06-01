package upc.com.pe.backendplannia.iam.infrastructure.acl;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.iam.domain.model.readmodels.UserTaskStatusCounts;
import upc.com.pe.backendplannia.iam.domain.services.UserTaskStatisticsPort;
import upc.com.pe.backendplannia.project.domain.model.queries.GetTaskStatusCountsByLatestAssignmentUserIdQuery;
import upc.com.pe.backendplannia.project.domain.services.TaskQueryService;

@Service
public class ProjectContextUserTaskStatisticsAdapter implements UserTaskStatisticsPort {
    private final TaskQueryService taskQueryService;

    public ProjectContextUserTaskStatisticsAdapter(TaskQueryService taskQueryService) {
        this.taskQueryService = taskQueryService;
    }

    @Override
    public UserTaskStatusCounts countByLatestAssignmentUserId(Long userId) {
        var counts = taskQueryService.handle(new GetTaskStatusCountsByLatestAssignmentUserIdQuery(userId));
        return new UserTaskStatusCounts(counts.toDoCount(), counts.inProgressCount(), counts.doneCount());
    }
}
