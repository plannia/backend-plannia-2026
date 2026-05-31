package upc.com.pe.backendplannia.assignment.application.internal.commandservices;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.assignment.application.internal.outboundservices.TaskRequirementGateway;
import upc.com.pe.backendplannia.assignment.domain.model.aggregates.Assignment;
import upc.com.pe.backendplannia.assignment.domain.model.commands.AutoAssignProjectCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.AutoAssignTeamCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.CompleteAssignmentCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.ConfirmRecommendationCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.CreateAssignmentCommand;
import upc.com.pe.backendplannia.assignment.domain.model.events.AssignmentCompletedEvent;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentCommandService;
import upc.com.pe.backendplannia.assignment.domain.services.CandidateProfileProvider;
import upc.com.pe.backendplannia.assignment.domain.services.MemberWorkloadPort;
import upc.com.pe.backendplannia.assignment.domain.services.ScoringDomainService;
import upc.com.pe.backendplannia.assignment.domain.model.valueobjects.AssignmentStatus;
import upc.com.pe.backendplannia.assignment.infrastructure.persistence.jpa.repositories.AssignmentRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AssignmentCommandServiceImpl implements AssignmentCommandService {
    private final AssignmentRepository assignmentRepository;
    private final CandidateProfileProvider candidateProfileProvider;
    private final MemberWorkloadPort memberWorkloadPort;
    private final TaskRequirementGateway taskRequirementGateway;
    private final ScoringDomainService scoringDomainService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AssignmentCommandServiceImpl(
            AssignmentRepository assignmentRepository,
            CandidateProfileProvider candidateProfileProvider,
            MemberWorkloadPort memberWorkloadPort,
            TaskRequirementGateway taskRequirementGateway,
            ScoringDomainService scoringDomainService,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.assignmentRepository = assignmentRepository;
        this.candidateProfileProvider = candidateProfileProvider;
        this.memberWorkloadPort = memberWorkloadPort;
        this.taskRequirementGateway = taskRequirementGateway;
        this.scoringDomainService = scoringDomainService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public Optional<Assignment> handle(CreateAssignmentCommand command) {
        ensureTaskHasNoActiveAssignment(command.taskId());
        var assignment = new Assignment(command);
        return Optional.of(assignmentRepository.save(assignment));
    }

    @Override
    public List<Assignment> handle(AutoAssignTeamCommand command) {
        throw new UnsupportedOperationException("Auto assign team is not implemented yet");
    }

    @Override
    public List<Assignment> handle(AutoAssignProjectCommand command) {
        throw new UnsupportedOperationException("Auto assign project is not implemented yet");
    }

    @Override
    @Transactional
    public Optional<Assignment> handle(ConfirmRecommendationCommand command) {
        ensureTaskHasNoActiveAssignment(command.taskId());

        var taskRequirement = taskRequirementGateway.requireByTaskId(command.taskId());
        var candidate = candidateProfileProvider.findByUserId(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("Member profile with this user id not found"));

        if (!scoringDomainService.meetsAvailabilityThreshold(candidate, taskRequirement)) {
            throw new IllegalArgumentException("Selected member does not meet availability threshold");
        }

        float skillMatch = scoringDomainService.calculateSkillMatch(candidate, taskRequirement);
        float experienceMatch = scoringDomainService.calculateExperienceMatch(candidate, taskRequirement);
        float interestMatch = scoringDomainService.calculateInterestMatch(candidate, taskRequirement);
        float score = scoringDomainService.calculateScore(candidate, taskRequirement);

        var createAssignmentCommand = new CreateAssignmentCommand(
                command.userId(),
                command.taskId(),
                skillMatch,
                experienceMatch,
                interestMatch,
                score
        );
        var savedAssignment = assignmentRepository.save(new Assignment(createAssignmentCommand));

        // Reservamos la carga del miembro al asignar; se libera al completar (ver handler del evento).
        memberWorkloadPort.reserveHours(command.userId(), taskRequirement.estimatedHours());

        return Optional.of(savedAssignment);
    }

    @Override
    @Transactional
    public Optional<Assignment> handle(CompleteAssignmentCommand command) {
        var assignment = assignmentRepository.findByTaskIdAndStatus(command.taskId(), AssignmentStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Active assignment with this task id not found"));

        assignment.complete();
        var savedAssignment = assignmentRepository.save(assignment);

        // Resolvemos aquí el requisito (horas + embedding) y lo enviamos en el evento; el handler
        // registra la experiencia y libera la carga del miembro dentro de esta misma transacción.
        var taskRequirement = taskRequirementGateway.requireByTaskId(savedAssignment.getTaskId());
        applicationEventPublisher.publishEvent(new AssignmentCompletedEvent(
                savedAssignment.getTaskId(),
                savedAssignment.getUserId(),
                taskRequirement.estimatedHours(),
                taskRequirement.requirementsEmbedding()
        ));

        return Optional.of(savedAssignment);
    }

    private void ensureTaskHasNoActiveAssignment(Long taskId) {
        if (assignmentRepository.existsByTaskIdAndStatus(taskId, AssignmentStatus.ACTIVE)) {
            throw new IllegalArgumentException("Task already has an active assignment");
        }
    }
}
