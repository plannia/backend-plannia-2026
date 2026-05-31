package upc.com.pe.backendplannia.assignment.application.internal.queryservices;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.assignment.application.internal.outboundservices.TaskRequirementGateway;
import upc.com.pe.backendplannia.assignment.domain.model.aggregates.Assignment;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetAssignmentsByUserIdQuery;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetTopCandidatesQuery;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentQueryService;
import upc.com.pe.backendplannia.assignment.domain.services.CandidateProfileProvider;
import upc.com.pe.backendplannia.assignment.domain.services.ScoringDomainService;
import upc.com.pe.backendplannia.assignment.infrastructure.persistence.jpa.repositories.AssignmentRepository;

import java.util.List;

@Service
public class AssignmentQueryServiceImpl implements AssignmentQueryService {
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
}
