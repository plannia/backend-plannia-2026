package upc.com.pe.backendplannia.project.domain.services;

import java.util.List;
import java.util.Optional;

public interface AssignmentActivityPort {
    boolean isLatestAssignmentUserActive(Long taskId);

    Optional<Long> findLatestAssignmentUserId(Long taskId);

    List<Long> findTaskIdsByLatestAssignmentUserId(Long userId);
}
