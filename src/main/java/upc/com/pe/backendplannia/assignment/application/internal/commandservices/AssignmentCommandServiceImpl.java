package upc.com.pe.backendplannia.assignment.application.internal.commandservices;

import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.assignment.application.internal.outboundservices.TaskRequirementGateway;
import upc.com.pe.backendplannia.assignment.domain.model.aggregates.Assignment;
import upc.com.pe.backendplannia.assignment.domain.model.commands.AutoAssignProjectCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.AutoAssignTeamCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.CompleteAssignmentCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.ConfirmRecommendationCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.CreateAssignmentCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.DeactivateUserAssignmentsCommand;
import upc.com.pe.backendplannia.assignment.domain.model.events.AssignmentCompletedEvent;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.AutoAssignmentResult;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.BacklogTask;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.TaskRequirement;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentCommandService;
import upc.com.pe.backendplannia.assignment.domain.services.CandidateProfileProvider;
import upc.com.pe.backendplannia.assignment.domain.services.MemberWorkloadPort;
import upc.com.pe.backendplannia.assignment.domain.services.ScoringDomainService;
import upc.com.pe.backendplannia.assignment.domain.services.TaskAssignmentPort;
import upc.com.pe.backendplannia.assignment.domain.services.UnassignedTaskPort;
import upc.com.pe.backendplannia.assignment.domain.model.valueobjects.AssignmentStatus;
import upc.com.pe.backendplannia.assignment.infrastructure.persistence.jpa.repositories.AssignmentRepository;
import upc.com.pe.backendplannia.shared.domain.model.events.MemberAssignedToTaskEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AssignmentCommandServiceImpl implements AssignmentCommandService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentCommandServiceImpl.class);

    // Orden del backlog: prioridad ↓, luego dificultad ↓, luego la que vence antes (limitDate ↑).
    private static final Comparator<BacklogTask> BACKLOG_ORDER =
            Comparator.comparingInt(BacklogTask::priorityRank).reversed()
                    .thenComparing(Comparator.comparingInt(BacklogTask::difficultyRank).reversed())
                    .thenComparing(BacklogTask::limitDate, Comparator.nullsLast(Comparator.naturalOrder()));

    private final AssignmentRepository assignmentRepository;
    private final CandidateProfileProvider candidateProfileProvider;
    private final MemberWorkloadPort memberWorkloadPort;
    private final TaskRequirementGateway taskRequirementGateway;
    private final ScoringDomainService scoringDomainService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TaskAssignmentPort taskAssignmentPort;
    private final UnassignedTaskPort unassignedTaskPort;

    public AssignmentCommandServiceImpl(
            AssignmentRepository assignmentRepository,
            CandidateProfileProvider candidateProfileProvider,
            MemberWorkloadPort memberWorkloadPort,
            TaskRequirementGateway taskRequirementGateway,
            ScoringDomainService scoringDomainService,
            ApplicationEventPublisher applicationEventPublisher,
            TaskAssignmentPort taskAssignmentPort,
            UnassignedTaskPort unassignedTaskPort
    ) {
        this.assignmentRepository = assignmentRepository;
        this.candidateProfileProvider = candidateProfileProvider;
        this.memberWorkloadPort = memberWorkloadPort;
        this.taskRequirementGateway = taskRequirementGateway;
        this.scoringDomainService = scoringDomainService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.taskAssignmentPort = taskAssignmentPort;
        this.unassignedTaskPort = unassignedTaskPort;
    }

    @Override
    @Transactional
    public Optional<Assignment> handle(CreateAssignmentCommand command) {
        var savedAssignment = assignmentRepository.save(new Assignment(command));
        taskAssignmentPort.markAsAssigned(savedAssignment.getTaskId());
        notifyMemberAssigned(savedAssignment);
        return Optional.of(savedAssignment);
    }

    // Auto-assign de TODO el equipo: reparte el backlog de todas sus categorías/proyectos en una pasada.
    @Override
    @Transactional
    public AutoAssignmentResult handle(AutoAssignTeamCommand command) {
        var candidates = candidateProfileProvider.findByTeamId(command.teamId());
        var backlog = unassignedTaskPort.findUnassignedByTeamId(command.teamId());
        return autoAssign(candidates, backlog);
    }

    // Auto-assign de un proyecto (categoría): mismo equipo como pool, solo las tareas de esa categoría.
    @Override
    @Transactional
    public AutoAssignmentResult handle(AutoAssignProjectCommand command) {
        var candidates = candidateProfileProvider.findByTeamId(command.teamId());
        var backlog = unassignedTaskPort.findUnassignedByCategoryId(command.projectId());
        return autoAssign(candidates, backlog);
    }

    @Override
    @Transactional
    public Optional<Assignment> handle(ConfirmRecommendationCommand command) {
        var taskRequirement = taskRequirementGateway.requireByTaskId(command.taskId());
        var candidate = candidateProfileProvider.findByUserId(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("Member profile with this user id not found"));

        if (!scoringDomainService.meetsAvailabilityThreshold(candidate, taskRequirement)) {
            throw new IllegalArgumentException("Selected member does not meet availability threshold");
        }

        return Optional.of(assignCandidateToTask(candidate, taskRequirement));
    }

    /**
     * Reparte un backlog entre los candidatos de forma greedy: ordena las tareas (prioridad → dificultad
     * → vencimiento), y a cada una le asigna el mejor candidato DISPONIBLE. La capacidad se lleva en
     * memoria (cada asignación descuenta horas del ganador), así las tareas siguientes ya lo ven más
     * cargado. Las tareas sin ningún candidato disponible se saltan y se reportan.
     */
    private AutoAssignmentResult autoAssign(List<CandidateProfile> candidates, List<BacklogTask> backlog) {
        // Estado mutable de capacidad por miembro: parte de su disponibilidad real y baja al reservar.
        Map<Long, CandidateProfile> liveCandidates = new LinkedHashMap<>();
        for (var candidate : candidates) {
            liveCandidates.put(candidate.userId(), candidate);
        }

        var assignments = new ArrayList<Assignment>();
        var skippedTaskIds = new ArrayList<Long>();

        var orderedBacklog = backlog.stream().sorted(BACKLOG_ORDER).toList();
        for (var backlogTask : orderedBacklog) {
            var winner = pickWinner(backlogTask, List.copyOf(liveCandidates.values()));
            if (winner.isEmpty()) {
                skippedTaskIds.add(backlogTask.taskId());
                continue;
            }
            var candidate = winner.get().candidate();
            assignments.add(assignCandidateToTask(candidate, winner.get().taskRequirement()));
            // Reflejamos la reserva en memoria: el ganador queda con menos horas para las próximas tareas.
            liveCandidates.put(
                    candidate.userId(),
                    withReservedHours(candidate, winner.get().taskRequirement().estimatedHours()));
        }

        return new AutoAssignmentResult(assignments, skippedTaskIds);
    }

    // Resuelve el requisito (embedding + horas) de la tarea y devuelve al mejor candidato disponible.
    // Si la tarea no se puede resolver/puntuar (p. ej. embedding incompatible), devuelve vacío (se salta).
    private Optional<RankedTask> pickWinner(BacklogTask backlogTask, List<CandidateProfile> candidates) {
        try {
            var taskRequirement = taskRequirementGateway.requireByTaskId(backlogTask.taskId());
            var ranked = scoringDomainService.rankCandidates(candidates, taskRequirement);
            if (ranked.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new RankedTask(ranked.getFirst(), taskRequirement));
        } catch (RuntimeException exception) {
            LOGGER.warn("Skipping task {} during auto-assign: {}", backlogTask.taskId(), exception.getMessage());
            return Optional.empty();
        }
    }

    // Crea la asignación, marca la tarea asignada, reserva la carga del miembro y notifica.
    // Compartido por la confirmación manual y el auto-assign para que ambos sigan la MISMA política.
    private Assignment assignCandidateToTask(CandidateProfile candidate, TaskRequirement taskRequirement) {
        float skillMatch = scoringDomainService.calculateSkillMatch(candidate, taskRequirement);
        float experienceMatch = scoringDomainService.calculateExperienceMatch(candidate, taskRequirement);
        float interestMatch = scoringDomainService.calculateInterestMatch(candidate, taskRequirement);
        float score = scoringDomainService.calculateScore(candidate, taskRequirement);

        var createAssignmentCommand = new CreateAssignmentCommand(
                candidate.userId(),
                taskRequirement.taskId(),
                skillMatch,
                experienceMatch,
                interestMatch,
                score
        );
        var savedAssignment = assignmentRepository.save(new Assignment(createAssignmentCommand));
        taskAssignmentPort.markAsAssigned(savedAssignment.getTaskId());

        // Reservamos la carga del miembro al asignar; se libera al completar (ver handler del evento).
        memberWorkloadPort.reserveHours(candidate.userId(), taskRequirement.estimatedHours());

        notifyMemberAssigned(savedAssignment);
        return savedAssignment;
    }

    private CandidateProfile withReservedHours(CandidateProfile candidate, int reservedHours) {
        return new CandidateProfile(
                candidate.userId(),
                candidate.embeddedAbilities(),
                candidate.embeddedExperience(),
                candidate.embeddedInterests(),
                candidate.activeHours() + reservedHours,
                candidate.maxHours()
        );
    }

    private record RankedTask(CandidateProfile candidate, TaskRequirement taskRequirement) {
    }

    @Override
    @Transactional
    public Optional<Assignment> handle(CompleteAssignmentCommand command) {
        LOGGER.info("Complete assignment requested: taskId={}", command.taskId());

        var assignment = assignmentRepository.findByTaskIdAndStatus(command.taskId(), AssignmentStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Active assignment with this task id not found"));
        LOGGER.info(
                "Active assignment found for completion: assignmentId={}, taskId={}, userId={}, status={}, active={}",
                assignment.getId(),
                assignment.getTaskId(),
                assignment.getUserId(),
                assignment.getStatus(),
                assignment.isActive()
        );

        assignment.complete();
        var savedAssignment = assignmentRepository.save(assignment);
        LOGGER.info(
                "Assignment marked completed: assignmentId={}, taskId={}, userId={}, status={}, active={}",
                savedAssignment.getId(),
                savedAssignment.getTaskId(),
                savedAssignment.getUserId(),
                savedAssignment.getStatus(),
                savedAssignment.isActive()
        );

        // Resolvemos aquí el requisito (horas + embedding) y lo enviamos en el evento; el handler
        // registra la experiencia y libera la carga del miembro dentro de esta misma transacción.
        var taskRequirement = taskRequirementGateway.requireByTaskId(savedAssignment.getTaskId());
        LOGGER.info(
                "Task requirement resolved for completed assignment: taskId={}, userId={}, estimatedHours={}, embeddingDim={}",
                savedAssignment.getTaskId(),
                savedAssignment.getUserId(),
                taskRequirement.estimatedHours(),
                taskRequirement.requirementsEmbedding().dimension()
        );
        applicationEventPublisher.publishEvent(new AssignmentCompletedEvent(
                savedAssignment.getTaskId(),
                savedAssignment.getUserId(),
                taskRequirement.estimatedHours(),
                taskRequirement.requirementsEmbedding()
        ));
        LOGGER.info(
                "AssignmentCompletedEvent published: taskId={}, userId={}",
                savedAssignment.getTaskId(),
                savedAssignment.getUserId()
        );

        return Optional.of(savedAssignment);
    }

    @Override
    @Transactional
    public List<Long> handle(DeactivateUserAssignmentsCommand command) {
        var assignments = assignmentRepository.findByUserId(command.userId());
        var affectedTaskIds = new LinkedHashSet<Long>();

        for (var assignment : assignments) {
            assignment.deactivate();
            affectedTaskIds.add(assignment.getTaskId());
        }

        assignmentRepository.saveAll(assignments);
        return new ArrayList<>(affectedTaskIds);
    }

    // Avisa "se asignó a un miembro a una tarea". El contexto Notifications escucha este evento
    // tras el commit (AFTER_COMMIT) para enviarle el correo; Assignment no conoce a Notifications.
    private void notifyMemberAssigned(Assignment assignment) {
        applicationEventPublisher.publishEvent(
                new MemberAssignedToTaskEvent(assignment.getUserId(), assignment.getTaskId()));
    }
}
