package upc.com.pe.backendplannia.assignment.domain.services;

import upc.com.pe.backendplannia.assignment.domain.model.aggregates.Assignment;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetAssignmentsByUserIdQuery;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetTopCandidatesQuery;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;

import java.util.List;

public interface AssignmentQueryService {
    List<CandidateProfile> handle(GetTopCandidatesQuery query);

    List<Assignment> handle(GetAssignmentsByUserIdQuery query);
}
