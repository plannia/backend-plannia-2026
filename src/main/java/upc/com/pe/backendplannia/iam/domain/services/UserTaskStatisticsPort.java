package upc.com.pe.backendplannia.iam.domain.services;

import upc.com.pe.backendplannia.iam.domain.model.readmodels.UserTaskStatusCounts;

public interface UserTaskStatisticsPort {
    UserTaskStatusCounts countByLatestAssignmentUserId(Long userId);
}
