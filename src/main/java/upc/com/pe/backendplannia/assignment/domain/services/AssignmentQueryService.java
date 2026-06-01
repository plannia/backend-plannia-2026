package upc.com.pe.backendplannia.assignment.domain.services;

import upc.com.pe.backendplannia.assignment.domain.model.aggregates.Assignment;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetAssignmentsByUserIdQuery;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetLatestAssignmentByTaskIdQuery;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetTaskIdsByLatestAssignmentUserIdQuery;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetTopCandidatesQuery;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.LatestAssignmentSnapshot;

import java.util.List;
import java.util.Optional;

public interface AssignmentQueryService {
    List<CandidateProfile> handle(GetTopCandidatesQuery query);

    List<Assignment> handle(GetAssignmentsByUserIdQuery query);

    Optional<LatestAssignmentSnapshot> handle(GetLatestAssignmentByTaskIdQuery query);

    List<Long> handle(GetTaskIdsByLatestAssignmentUserIdQuery query);
}
