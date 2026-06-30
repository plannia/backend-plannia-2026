package upc.com.pe.backendplannia.assignment.application.internal.queryservices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.assignment.application.internal.outboundservices.TaskRequirementGateway;
import upc.com.pe.backendplannia.assignment.domain.model.aggregates.Assignment;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetAssignmentsByUserIdQuery;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetLatestAssignmentByTaskIdQuery;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetTaskIdsByLatestAssignmentUserIdQuery;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetTopCandidatesQuery;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.LatestAssignmentSnapshot;
import upc.com.pe.backendplannia.assignment.domain.model.valueobjects.AssignmentStatus;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentQueryService;
import upc.com.pe.backendplannia.assignment.domain.services.CandidateProfileProvider;
import upc.com.pe.backendplannia.assignment.domain.services.ScoringDomainService;
import upc.com.pe.backendplannia.assignment.infrastructure.persistence.jpa.repositories.AssignmentRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AssignmentQueryServiceImpl implements AssignmentQueryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentQueryServiceImpl.class);

    private static final int TOP_CANDIDATES_LIMIT = 3;

    private final CandidateProfileProvider candidateProfileProvider;
    private final AssignmentRepository assignmentRepository;
    private final TaskRequirementGateway taskRequirementGateway;
    private final ScoringDomainService scoringDomainService;

    public AssignmentQueryServiceImpl(
            CandidateProfileProvider candidateProfileProvider,
            AssignmentRepository assignmentRepository,
            TaskRequirementGateway taskRequirementGateway,
            ScoringDomainService scoringDomainService
    ) {
        this.candidateProfileProvider = candidateProfileProvider;
        this.assignmentRepository = assignmentRepository;
        this.taskRequirementGateway = taskRequirementGateway;
        this.scoringDomainService = scoringDomainService;
    }

    @Override
    public List<CandidateProfile> handle(GetTopCandidatesQuery query) {
        var taskRequirement = taskRequirementGateway.requireByTaskId(query.taskId());
        var candidates = candidateProfileProvider.findByTeamId(query.teamId());

        return scoringDomainService.rankCandidates(candidates, taskRequirement).stream()
                .limit(TOP_CANDIDATES_LIMIT)
                .toList();
    }

    @Override
    public List<Assignment> handle(GetAssignmentsByUserIdQuery query) {
        return assignmentRepository.findByUserId(query.userId());
    }

    @Override
    public Optional<LatestAssignmentSnapshot> handle(GetLatestAssignmentByTaskIdQuery query) {
        return assignmentRepository.findFirstByTaskIdOrderByCreatedAtDesc(query.taskId())
                .map(assignment -> {
                    boolean effectiveActive = assignment.getStatus() == AssignmentStatus.ACTIVE && assignment.isActive();
                    LOGGER.info(
                            "Latest assignment snapshot resolved: assignmentId={}, taskId={}, userId={}, status={}, rawActive={}, effectiveActive={}",
                            assignment.getId(),
                            assignment.getTaskId(),
                            assignment.getUserId(),
                            assignment.getStatus(),
                            assignment.isActive(),
                            effectiveActive
                    );
                    return new LatestAssignmentSnapshot(
                            assignment.getUserId(),
                            effectiveActive
                    );
                });
    }

    @Override
    public List<Long> handle(GetTaskIdsByLatestAssignmentUserIdQuery query) {
        return assignmentRepository.findLatestAssignmentsByUserId(query.userId()).stream()
                .map(Assignment::getTaskId)
                .toList();
    }
}
