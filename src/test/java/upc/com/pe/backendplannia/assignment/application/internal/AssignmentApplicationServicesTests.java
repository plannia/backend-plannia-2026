package upc.com.pe.backendplannia.assignment.application.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import upc.com.pe.backendplannia.assignment.application.internal.commandservices.AssignmentCommandServiceImpl;
import upc.com.pe.backendplannia.assignment.application.internal.outboundservices.TaskRequirementGateway;
import upc.com.pe.backendplannia.assignment.application.internal.queryservices.AssignmentQueryServiceImpl;
import upc.com.pe.backendplannia.assignment.domain.model.aggregates.Assignment;
import upc.com.pe.backendplannia.assignment.domain.model.commands.AutoAssignProjectCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.AutoAssignTeamCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.CompleteAssignmentCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.ConfirmRecommendationCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.CreateAssignmentCommand;
import upc.com.pe.backendplannia.assignment.domain.model.events.AssignmentCompletedEvent;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetAssignmentsByUserIdQuery;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetTopCandidatesQuery;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.BacklogTask;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.TaskRequirement;
import upc.com.pe.backendplannia.assignment.domain.model.valueobjects.AssignmentStatus;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentCommandService;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentQueryService;
import upc.com.pe.backendplannia.assignment.domain.services.CandidateProfileProvider;
import upc.com.pe.backendplannia.assignment.domain.services.MemberWorkloadPort;
import upc.com.pe.backendplannia.assignment.domain.services.ScoringDomainService;
import upc.com.pe.backendplannia.assignment.domain.services.TaskAssignmentPort;
import upc.com.pe.backendplannia.assignment.domain.services.TaskRequirementResolver;
import upc.com.pe.backendplannia.assignment.domain.services.UnassignedTaskPort;
import upc.com.pe.backendplannia.assignment.infrastructure.persistence.jpa.repositories.AssignmentRepository;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
@ContextConfiguration(classes = {
        AssignmentCommandServiceImpl.class,
        AssignmentQueryServiceImpl.class,
        ScoringDomainService.class,
        TaskRequirementGateway.class
})
@RecordApplicationEvents
class AssignmentApplicationServicesTests {
    private static final Long TASK_ID = 501L;
    private static final Long TEAM_ID = 301L;
    private static final Long BEST_USER_ID = 101L;
    private static final Long SECOND_USER_ID = 102L;
    private static final Long THIRD_USER_ID = 103L;
    private static final Long FOURTH_USER_ID = 104L;

    @Autowired
    private AssignmentCommandService assignmentCommandService;

    @Autowired
    private AssignmentQueryService assignmentQueryService;

    @Autowired
    private ApplicationEvents applicationEvents;

    @MockitoBean
    private AssignmentRepository assignmentRepository;

    @MockitoBean
    private CandidateProfileProvider candidateProfileProvider;

    @MockitoBean
    private MemberWorkloadPort memberWorkloadPort;

    // El ObjectProvider de TaskRequirementGateway resuelve este mock como bean disponible.
    @MockitoBean
    private TaskRequirementResolver taskRequirementResolver;

    @MockitoBean
    private TaskAssignmentPort taskAssignmentPort;

    @MockitoBean
    private UnassignedTaskPort unassignedTaskPort;

    @Test
    void handleCreateAssignmentCommandCreatesActiveAssignment() {
        var command = new CreateAssignmentCommand(SECOND_USER_ID, TASK_ID, 0.80f, 0.60f, 0.40f, 0.69f);

        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = assignmentCommandService.handle(command);

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(SECOND_USER_ID);
        assertThat(result.get().getTaskId()).isEqualTo(TASK_ID);
        assertThat(result.get().getSkillMatch()).isEqualTo(0.80f);
        assertThat(result.get().getExperienceMatch()).isEqualTo(0.60f);
        assertThat(result.get().getInterestMatch()).isEqualTo(0.40f);
        assertThat(result.get().getScore()).isEqualTo(0.69f);
        assertThat(result.get().getStatus()).isEqualTo(AssignmentStatus.ACTIVE);
        assertThat(result.get().isActive()).isTrue();
        verify(taskAssignmentPort).markAsAssigned(TASK_ID);
    }

    @Test
    void handleAutoAssignTeamCommandAssignsEachTaskToBestCandidateInPriorityOrder() {
        var now = LocalDateTime.now();
        // Backend y diseñador, ambos con 8h libres. Cada uno gana la tarea de su especialidad.
        var backend = candidate(BEST_USER_ID, vector(1f, 0f, 0f), vector(1f, 0f, 0f), vector(1f, 0f, 0f), 0f, 8f);
        var designer = candidate(THIRD_USER_ID, vector(0f, 1f, 0f), vector(0f, 1f, 0f), vector(0f, 1f, 0f), 0f, 8f);

        var backendTask = new TaskRequirement(TASK_ID, vector(1f, 0f, 0f), 2, "HIGH", "MEDIUM");
        var designTask = new TaskRequirement(502L, vector(0f, 1f, 0f), 2, "LOW", "EASY");

        when(candidateProfileProvider.findByTeamId(TEAM_ID)).thenReturn(List.of(backend, designer));
        // El backlog llega desordenado: HIGH (TASK_ID) debe procesarse antes que LOW (502).
        when(unassignedTaskPort.findUnassignedByTeamId(TEAM_ID)).thenReturn(List.of(
                new BacklogTask(502L, 1, 1, now.plusDays(5)),
                new BacklogTask(TASK_ID, 3, 2, now.plusDays(1))
        ));
        when(taskRequirementResolver.resolveByTaskId(TASK_ID)).thenReturn(Optional.of(backendTask));
        when(taskRequirementResolver.resolveByTaskId(502L)).thenReturn(Optional.of(designTask));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = assignmentCommandService.handle(new AutoAssignTeamCommand(TEAM_ID));

        assertThat(result.skippedTaskIds()).isEmpty();
        // Procesadas en orden de prioridad: backend (HIGH) primero, diseño (LOW) después.
        assertThat(result.assignments())
                .extracting(Assignment::getTaskId)
                .containsExactly(TASK_ID, 502L);
        assertThat(result.assignments())
                .extracting(Assignment::getUserId)
                .containsExactly(BEST_USER_ID, THIRD_USER_ID);
        verify(taskAssignmentPort).markAsAssigned(TASK_ID);
        verify(taskAssignmentPort).markAsAssigned(502L);
        verify(memberWorkloadPort).reserveHours(BEST_USER_ID, backendTask.estimatedHours());
        verify(memberWorkloadPort).reserveHours(THIRD_USER_ID, designTask.estimatedHours());
    }

    @Test
    void handleAutoAssignTeamCommandRespectsCumulativeCapacityAndSkipsTasksWithoutCandidate() {
        var now = LocalDateTime.now();
        // Único candidato con 5h libres. Dos tareas de 3h: solo entra la primera; la segunda se salta.
        var backend = candidate(BEST_USER_ID, vector(1f, 0f, 0f), vector(1f, 0f, 0f), vector(1f, 0f, 0f), 0f, 5f);

        var firstTask = new TaskRequirement(TASK_ID, vector(1f, 0f, 0f), 3, "HIGH", "HARD");
        var secondTask = new TaskRequirement(502L, vector(1f, 0f, 0f), 3, "HIGH", "HARD");

        when(candidateProfileProvider.findByTeamId(TEAM_ID)).thenReturn(List.of(backend));
        // Misma prioridad/dificultad: desempata limitDate. TASK_ID vence antes → se procesa primero.
        when(unassignedTaskPort.findUnassignedByTeamId(TEAM_ID)).thenReturn(List.of(
                new BacklogTask(502L, 3, 3, now.plusDays(5)),
                new BacklogTask(TASK_ID, 3, 3, now.plusDays(1))
        ));
        when(taskRequirementResolver.resolveByTaskId(TASK_ID)).thenReturn(Optional.of(firstTask));
        when(taskRequirementResolver.resolveByTaskId(502L)).thenReturn(Optional.of(secondTask));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = assignmentCommandService.handle(new AutoAssignTeamCommand(TEAM_ID));

        // La primera tarea se asigna; la segunda queda sin candidato disponible (3h > 2h restantes).
        assertThat(result.assignments())
                .extracting(Assignment::getTaskId)
                .containsExactly(TASK_ID);
        assertThat(result.skippedTaskIds()).containsExactly(502L);
        verify(memberWorkloadPort).reserveHours(BEST_USER_ID, firstTask.estimatedHours());
        verify(taskAssignmentPort).markAsAssigned(TASK_ID);
        verify(taskAssignmentPort, never()).markAsAssigned(502L);
    }

    @Test
    void handleAutoAssignProjectCommandUsesCategoryBacklog() {
        var backend = candidate(BEST_USER_ID, vector(1f, 0f, 0f), vector(1f, 0f, 0f), vector(1f, 0f, 0f), 0f, 8f);
        var task = new TaskRequirement(TASK_ID, vector(1f, 0f, 0f), 2, "MEDIUM", "MEDIUM");

        when(candidateProfileProvider.findByTeamId(TEAM_ID)).thenReturn(List.of(backend));
        when(unassignedTaskPort.findUnassignedByCategoryId(401L)).thenReturn(List.of(
                new BacklogTask(TASK_ID, 2, 2, LocalDateTime.now().plusDays(2))
        ));
        when(taskRequirementResolver.resolveByTaskId(TASK_ID)).thenReturn(Optional.of(task));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = assignmentCommandService.handle(new AutoAssignProjectCommand(TEAM_ID, 401L));

        assertThat(result.skippedTaskIds()).isEmpty();
        assertThat(result.assignments())
                .extracting(Assignment::getUserId)
                .containsExactly(BEST_USER_ID);
        // Scope de proyecto: backlog por categoría, NO por equipo.
        verify(unassignedTaskPort).findUnassignedByCategoryId(401L);
    }

    @Test
    void handleConfirmRecommendationCommandCreatesAssignmentWithCalculatedScoresAndReservesWorkload() {
        var taskRequirement = taskRequirement();
        var selectedCandidate = candidate(
                SECOND_USER_ID,
                vector(1f, 0f, 0f),
                vector(0f, 1f, 0f),
                vector(1f, 0f, 0f),
                1f,
                8f
        );

        when(taskRequirementResolver.resolveByTaskId(TASK_ID)).thenReturn(Optional.of(taskRequirement));
        when(candidateProfileProvider.findByUserId(SECOND_USER_ID)).thenReturn(Optional.of(selectedCandidate));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = assignmentCommandService.handle(new ConfirmRecommendationCommand(TASK_ID, SECOND_USER_ID));

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(SECOND_USER_ID);
        assertThat(result.get().getTaskId()).isEqualTo(TASK_ID);
        assertThat(result.get().getSkillMatch()).isEqualTo(1f);
        assertThat(result.get().getExperienceMatch()).isEqualTo(0f);
        assertThat(result.get().getInterestMatch()).isEqualTo(1f);
        assertThat(result.get().getScore()).isCloseTo(0.65f, offset(0.0001f));
        assertThat(result.get().getStatus()).isEqualTo(AssignmentStatus.ACTIVE);
        // La carga del miembro se reserva por las horas estimadas de la tarea.
        verify(memberWorkloadPort).reserveHours(SECOND_USER_ID, taskRequirement.estimatedHours());
        verify(taskAssignmentPort).markAsAssigned(TASK_ID);
    }

    @Test
    void handleConfirmRecommendationCommandRejectsUnknownMemberAndDoesNotReserveWorkload() {
        var taskRequirement = taskRequirement();

        when(taskRequirementResolver.resolveByTaskId(TASK_ID)).thenReturn(Optional.of(taskRequirement));
        when(candidateProfileProvider.findByUserId(SECOND_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentCommandService.handle(
                new ConfirmRecommendationCommand(TASK_ID, SECOND_USER_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Member profile with this user id not found");
        verify(assignmentRepository, never()).save(any(Assignment.class));
        verify(memberWorkloadPort, never()).reserveHours(anyLong(), anyFloat());
    }

    @Test
    void handleConfirmRecommendationCommandRejectsMemberWithoutAvailability() {
        var taskRequirement = taskRequirement();
        // estimatedHours = 4, pero solo le quedan 3 horas disponibles (8 - 5).
        var overloadedCandidate = candidate(
                SECOND_USER_ID,
                vector(1f, 0f, 0f),
                vector(1f, 0f, 0f),
                vector(1f, 0f, 0f),
                5f,
                8f
        );

        when(taskRequirementResolver.resolveByTaskId(TASK_ID)).thenReturn(Optional.of(taskRequirement));
        when(candidateProfileProvider.findByUserId(SECOND_USER_ID)).thenReturn(Optional.of(overloadedCandidate));

        assertThatThrownBy(() -> assignmentCommandService.handle(
                new ConfirmRecommendationCommand(TASK_ID, SECOND_USER_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Selected member does not meet availability threshold");
        verify(assignmentRepository, never()).save(any(Assignment.class));
        verify(memberWorkloadPort, never()).reserveHours(anyLong(), anyFloat());
    }

    @Test
    void handleCompleteAssignmentCommandCompletesActiveAssignmentAndPublishesEvent() {
        var assignment = new Assignment(new CreateAssignmentCommand(BEST_USER_ID, TASK_ID, 1f, 1f, 1f, 1f));
        var taskRequirement = taskRequirement();

        when(assignmentRepository.findByTaskIdAndStatus(TASK_ID, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRequirementResolver.resolveByTaskId(TASK_ID)).thenReturn(Optional.of(taskRequirement));

        var result = assignmentCommandService.handle(new CompleteAssignmentCommand(TASK_ID));

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
        // El evento ahora transporta horas y embedding para que el handler actualice el perfil.
        assertThat(applicationEvents.stream(AssignmentCompletedEvent.class))
                .contains(new AssignmentCompletedEvent(
                        TASK_ID,
                        BEST_USER_ID,
                        taskRequirement.estimatedHours(),
                        taskRequirement.requirementsEmbedding()
                ));
    }

    @Test
    void handleCompleteAssignmentCommandRejectsTaskWithoutActiveAssignment() {
        when(assignmentRepository.findByTaskIdAndStatus(TASK_ID, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentCommandService.handle(new CompleteAssignmentCommand(TASK_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Active assignment with this task id not found");
        verify(assignmentRepository, never()).save(any(Assignment.class));
        assertThat(applicationEvents.stream(AssignmentCompletedEvent.class)).isEmpty();
    }

    @Test
    void handleGetTopCandidatesQueryReturnsThreeProfilesOrderedByScore() {
        var taskRequirement = taskRequirement();
        var bestCandidate = candidate(
                BEST_USER_ID,
                vector(1f, 0f, 0f),
                vector(1f, 0f, 0f),
                vector(1f, 0f, 0f),
                2f,
                8f
        );
        var secondCandidate = candidate(
                SECOND_USER_ID,
                vector(1f, 0f, 0f),
                vector(0f, 1f, 0f),
                vector(1f, 0f, 0f),
                1f,
                8f
        );
        var thirdCandidate = candidate(
                THIRD_USER_ID,
                vector(0f, 1f, 0f),
                vector(1f, 0f, 0f),
                vector(0f, 1f, 0f),
                0f,
                8f
        );

        when(taskRequirementResolver.resolveByTaskId(TASK_ID)).thenReturn(Optional.of(taskRequirement));
        when(candidateProfileProvider.findByTeamId(TEAM_ID))
                .thenReturn(List.of(thirdCandidate, secondCandidate, bestCandidate));

        var result = assignmentQueryService.handle(new GetTopCandidatesQuery(TASK_ID, TEAM_ID));

        assertThat(result)
                .extracting(CandidateProfile::userId)
                .containsExactly(BEST_USER_ID, SECOND_USER_ID, THIRD_USER_ID);
    }

    @Test
    void handleGetTopCandidatesQueryLimitsRankingToThree() {
        var taskRequirement = taskRequirement();
        var best = candidate(BEST_USER_ID, vector(1f, 0f, 0f), vector(1f, 0f, 0f), vector(1f, 0f, 0f), 0f, 8f);   // score 1.00
        var second = candidate(SECOND_USER_ID, vector(1f, 0f, 0f), vector(0f, 1f, 0f), vector(1f, 0f, 0f), 0f, 8f); // score 0.65
        var third = candidate(THIRD_USER_ID, vector(1f, 0f, 0f), vector(0f, 1f, 0f), vector(0f, 1f, 0f), 0f, 8f);  // score 0.55
        var fourth = candidate(FOURTH_USER_ID, vector(0f, 1f, 0f), vector(0f, 1f, 0f), vector(0f, 1f, 0f), 0f, 8f); // score 0.00

        when(taskRequirementResolver.resolveByTaskId(TASK_ID)).thenReturn(Optional.of(taskRequirement));
        when(candidateProfileProvider.findByTeamId(TEAM_ID))
                .thenReturn(List.of(fourth, third, best, second));

        var result = assignmentQueryService.handle(new GetTopCandidatesQuery(TASK_ID, TEAM_ID));

        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting(CandidateProfile::userId)
                .containsExactly(BEST_USER_ID, SECOND_USER_ID, THIRD_USER_ID);
    }

    @Test
    void handleGetTopCandidatesQueryExcludesMembersWithoutAvailability() {
        var taskRequirement = taskRequirement();
        var available = candidate(BEST_USER_ID, vector(1f, 0f, 0f), vector(1f, 0f, 0f), vector(1f, 0f, 0f), 0f, 8f);
        // estimatedHours = 4, pero solo le quedan 2 horas disponibles (8 - 6): debe quedar fuera del ranking.
        var overloaded = candidate(SECOND_USER_ID, vector(1f, 0f, 0f), vector(1f, 0f, 0f), vector(1f, 0f, 0f), 6f, 8f);

        when(taskRequirementResolver.resolveByTaskId(TASK_ID)).thenReturn(Optional.of(taskRequirement));
        when(candidateProfileProvider.findByTeamId(TEAM_ID))
                .thenReturn(List.of(overloaded, available));

        var result = assignmentQueryService.handle(new GetTopCandidatesQuery(TASK_ID, TEAM_ID));

        assertThat(result)
                .extracting(CandidateProfile::userId)
                .containsExactly(BEST_USER_ID);
    }

    @Test
    void handleGetAssignmentsByUserIdQueryReturnsAssignmentsForMember() {
        var activeAssignment = new Assignment(new CreateAssignmentCommand(BEST_USER_ID, TASK_ID, 1f, 1f, 1f, 1f));
        var completedAssignment = new Assignment(new CreateAssignmentCommand(BEST_USER_ID, 502L, 0.55f, 0.35f, 0.10f, 0.435f));
        completedAssignment.complete();

        when(assignmentRepository.findByUserId(BEST_USER_ID))
                .thenReturn(List.of(activeAssignment, completedAssignment));

        var result = assignmentQueryService.handle(new GetAssignmentsByUserIdQuery(BEST_USER_ID));

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Assignment::getTaskId)
                .containsExactly(TASK_ID, 502L);
    }

    private TaskRequirement taskRequirement() {
        return new TaskRequirement(TASK_ID, vector(1f, 0f, 0f), 4, "HIGH", "MEDIUM");
    }

    private CandidateProfile candidate(
            Long userId,
            EmbeddingVector embeddedAbilities,
            EmbeddingVector embeddedExperience,
            EmbeddingVector embeddedInterests,
            float activeHours,
            float maxHours
    ) {
        return new CandidateProfile(
                userId,
                embeddedAbilities,
                embeddedExperience,
                embeddedInterests,
                activeHours,
                maxHours
        );
    }

    private EmbeddingVector vector(float... values) {
        return EmbeddingVector.of(
                java.util.stream.IntStream.range(0, values.length)
                        .mapToObj(index -> values[index])
                        .toList()
        );
    }
}
