package upc.com.pe.backendplannia.assignment.domain.services;

import upc.com.pe.backendplannia.assignment.domain.model.aggregates.Assignment;
import upc.com.pe.backendplannia.assignment.domain.model.commands.AutoAssignProjectCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.AutoAssignTeamCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.CompleteAssignmentCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.ConfirmRecommendationCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.CreateAssignmentCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.DeactivateUserAssignmentsCommand;

import java.util.List;
import java.util.Optional;

public interface AssignmentCommandService {
    Optional<Assignment> handle(CreateAssignmentCommand command);

    List<Assignment> handle(AutoAssignTeamCommand command);

    List<Assignment> handle(AutoAssignProjectCommand command);

    Optional<Assignment> handle(ConfirmRecommendationCommand command);

    Optional<Assignment> handle(CompleteAssignmentCommand command);

    List<Long> handle(DeactivateUserAssignmentsCommand command);
}
