package upc.com.pe.backendplannia.project.infrastructure.acl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetLatestAssignmentByTaskIdQuery;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetTaskIdsByLatestAssignmentUserIdQuery;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.LatestAssignmentSnapshot;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentQueryService;
import upc.com.pe.backendplannia.project.domain.services.AssignmentActivityPort;

import java.util.List;
import java.util.Optional;

@Service
public class AssignmentContextActivityAdapter implements AssignmentActivityPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentContextActivityAdapter.class);

    private final AssignmentQueryService assignmentQueryService;

    public AssignmentContextActivityAdapter(AssignmentQueryService assignmentQueryService) {
        this.assignmentQueryService = assignmentQueryService;
    }

    @Override
    public boolean isLatestAssignmentUserActive(Long taskId) {
        var snapshot = assignmentQueryService.handle(new GetLatestAssignmentByTaskIdQuery(taskId));
        LOGGER.info(
                "Resolved latest assignment active state: taskId={}, found={}, userId={}, active={}",
                taskId,
                snapshot.isPresent(),
                snapshot.map(LatestAssignmentSnapshot::userId).orElse(null),
                snapshot.map(LatestAssignmentSnapshot::isActive).orElse(false)
        );
        return snapshot.map(LatestAssignmentSnapshot::isActive).orElse(false);
    }

    @Override
    public Optional<Long> findLatestAssignmentUserId(Long taskId) {
        var snapshot = assignmentQueryService.handle(new GetLatestAssignmentByTaskIdQuery(taskId));
        LOGGER.info(
                "Resolved latest assignment user: taskId={}, found={}, userId={}, active={}",
                taskId,
                snapshot.isPresent(),
                snapshot.map(LatestAssignmentSnapshot::userId).orElse(null),
                snapshot.map(LatestAssignmentSnapshot::isActive).orElse(false)
        );
        return snapshot.map(LatestAssignmentSnapshot::userId);
    }

    @Override
    public List<Long> findTaskIdsByLatestAssignmentUserId(Long userId) {
        return assignmentQueryService.handle(new GetTaskIdsByLatestAssignmentUserIdQuery(userId));
    }
}
