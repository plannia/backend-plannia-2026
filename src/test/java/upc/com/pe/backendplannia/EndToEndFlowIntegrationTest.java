package upc.com.pe.backendplannia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import upc.com.pe.backendplannia.assignment.domain.model.commands.CompleteAssignmentCommand;
import upc.com.pe.backendplannia.assignment.domain.model.commands.ConfirmRecommendationCommand;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetTopCandidatesQuery;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;
import upc.com.pe.backendplannia.assignment.domain.model.valueobjects.AssignmentStatus;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentCommandService;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentQueryService;
import upc.com.pe.backendplannia.assignment.infrastructure.persistence.jpa.repositories.AssignmentRepository;
import upc.com.pe.backendplannia.iam.domain.model.aggregates.Team;
import upc.com.pe.backendplannia.iam.domain.model.commands.CreateTeamCommand;
import upc.com.pe.backendplannia.iam.domain.model.commands.DeleteUserCommand;
import upc.com.pe.backendplannia.iam.domain.model.commands.SignInCommand;
import upc.com.pe.backendplannia.iam.domain.model.commands.SignUpCommand;
import upc.com.pe.backendplannia.iam.domain.services.TeamCommandService;
import upc.com.pe.backendplannia.iam.domain.services.UserCommandService;
import upc.com.pe.backendplannia.notifications.domain.model.queries.GetNotificationsByUserIdQuery;
import upc.com.pe.backendplannia.notifications.domain.services.NotificationQueryService;
import upc.com.pe.backendplannia.profile.application.internal.outboundservices.ai.ProfileEmbeddingService;
import upc.com.pe.backendplannia.profile.domain.model.commands.UpdateMemberProfileCommand;
import upc.com.pe.backendplannia.profile.domain.services.MemberProfileCommandService;
import upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories.MemberProfileRepository;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateCategoryCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateTaskCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.UpdateTaskCommand;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;
import upc.com.pe.backendplannia.project.domain.services.CategoryCommandService;
import upc.com.pe.backendplannia.project.domain.services.TaskCommandService;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories.TaskRepository;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test de integración END-TO-END de todos los flujos, con el contexto completo de Spring sobre H2
 * en memoria (sin Postgres ni Azure). Los embeddings se simulan de forma DETERMINISTA (bag-of-words)
 * para que el ranking sea reproducible sin llamar a la IA.
 *
 * Verifica la automatización que cruza contextos vía eventos/ACL:
 *  - alta + login (IAM),
 *  - recomendación por afinidad (Assignment + Profile),
 *  - confirmar → reserva de carga (Profile) + tarea marcada asignada (Project) + notificación (Notifications),
 *  - marcar tarea DONE → AssignmentCompleted: libera carga + registra experiencia,
 *  - borrar usuario → desactiva asignaciones + limpia categorías.
 *
 * Marca con "GAP" los huecos de automatización detectados.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:plannia-e2e;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "JWT_SECRET=integration-test-secret-please-32-chars",
        "notifications.email.transport=log",
        "gantt.google.enabled=false"
})
class EndToEndFlowIntegrationTest {
    private static final AtomicInteger SEQ = new AtomicInteger();

    private static final String BACKEND_ABILITIES = "Java Spring Boot backend REST API and JPA repositories";
    private static final String DESIGN_ABILITIES = "Figma design UI CSS visual frontend prototyping";

    @Autowired private TeamCommandService teamCommandService;
    @Autowired private UserCommandService userCommandService;
    @Autowired private MemberProfileCommandService memberProfileCommandService;
    @Autowired private CategoryCommandService categoryCommandService;
    @Autowired private TaskCommandService taskCommandService;
    @Autowired private AssignmentQueryService assignmentQueryService;
    @Autowired private AssignmentCommandService assignmentCommandService;
    @Autowired private NotificationQueryService notificationQueryService;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    // Embeddings deterministas: el vector refleja qué palabras clave contiene el texto, así la
    // similitud coseno ≈ solapamiento de keywords (un backend matchea una tarea backend, etc.).
    @MockitoBean private ProfileEmbeddingService profileEmbeddingService;

    @BeforeEach
    void stubDeterministicEmbeddings() {
        when(profileEmbeddingService.generateEmbedding(anyString()))
                .thenAnswer(invocation -> deterministicEmbedding(invocation.getArgument(0)));
    }

    // ---------------------------------------------------------------------------------------------
    // FLUJO 1 — Autenticación: alta de miembro y login
    // ---------------------------------------------------------------------------------------------
    @Test
    @DisplayName("Login: alta de miembro y sign-in emiten token; credenciales malas → vacío")
    void authenticationFlow() {
        var team = createTeam();
        var memberEmail = uniqueEmail("member");
        signUpMember(team, memberEmail, "Backend Dev");

        var ok = userCommandService.handle(new SignInCommand(memberEmail, "password123"));
        assertThat(ok).isPresent();
        assertThat(ok.get().getRight()).isNotBlank(); // token JWT

        var bad = userCommandService.handle(new SignInCommand(memberEmail, "wrong"));
        assertThat(bad).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------
    // FLUJO 2 — Recomendación + confirmar: ranking, reserva de carga, tarea asignada, notificación
    // ---------------------------------------------------------------------------------------------
    @Test
    @DisplayName("Confirmar asignación: rankea al mejor, reserva carga, marca tarea asignada y notifica")
    void recommendConfirmReservesWorkloadAndNotifies() {
        var team = createTeam();
        var backendUserId = onboardMember(team, "Backend Dev", BACKEND_ABILITIES, 40f);
        var designerUserId = onboardMember(team, "Designer", DESIGN_ABILITIES, 40f);
        var taskId = createBackendTask(team, 4);

        // El ranking debe poner al backend primero para una tarea backend.
        List<CandidateProfile> ranking = topCandidates(taskId, team.getId());
        assertThat(ranking).extracting(CandidateProfile::userId).first().isEqualTo(backendUserId);

        // Confirmar la recomendación del backend.
        var assignment = assignmentCommandService.handle(new ConfirmRecommendationCommand(taskId, backendUserId));
        assertThat(assignment).isPresent();
        assertThat(assignment.get().getStatus()).isEqualTo(AssignmentStatus.ACTIVE);

        // Automatización cruzada:
        // (a) Project: la tarea quedó marcada como asignada.
        assertThat(taskRepository.findById(taskId)).get().extracting("isAssigned").isEqualTo(true);
        // (b) Profile: se reservaron las horas de la tarea (activeHours = 4).
        assertThat(memberProfileRepository.findByUserId(backendUserId)).get()
                .extracting(p -> p.getActiveHours()).isEqualTo(4f);
        // (c) Notifications: la notificación se crea de forma ASÍNCRONA (AFTER_COMMIT + @Async),
        //     así que esperamos a que el listener termine en su propio hilo (es eventual, no inmediata).
        awaitNotificationCount(backendUserId, 1);
        // El diseñador no fue tocado.
        assertThat(memberProfileRepository.findByUserId(designerUserId)).get()
                .extracting(p -> p.getActiveHours()).isEqualTo(0f);
    }

    // ---------------------------------------------------------------------------------------------
    // FLUJO 3 — Marcar tarea DONE → evento auto-completa la asignación, libera carga y registra experiencia
    // ---------------------------------------------------------------------------------------------
    @Test
    @DisplayName("Marcar tarea DONE: auto-completa la asignación, libera la carga y registra experiencia")
    void markingTaskDoneAutoCompletesAssignment() {
        var team = createTeam();
        var backendUserId = onboardMember(team, "Backend Dev", BACKEND_ABILITIES, 40f);
        var taskId = createBackendTask(team, 4);
        assignmentCommandService.handle(new ConfirmRecommendationCommand(taskId, backendUserId));

        // Antes: asignación ACTIVE, carga 4, sin experiencia embebida.
        assertThat(memberProfileRepository.findByUserId(backendUserId).orElseThrow().getActiveHours()).isEqualTo(4f);
        assertThat(memberProfileRepository.findByUserId(backendUserId).orElseThrow()
                .getEmbeddedExperience().dimension()).isZero();

        // Acción: el líder marca la tarea como DONE → dispara TaskMarkedAsDoneEvent.
        taskCommandService.handle(new UpdateTaskCommand(taskId, "DONE", null));

        // Después (automático por eventos):
        var assignment = assignmentRepository.findByTaskId(taskId).getFirst();
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.COMPLETED); // completada sola
        var profile = memberProfileRepository.findByUserId(backendUserId).orElseThrow();
        assertThat(profile.getActiveHours()).isEqualTo(0f);                       // carga liberada
        assertThat(profile.getEmbeddedExperience().dimension()).isGreaterThan(0); // experiencia registrada
    }

    // ---------------------------------------------------------------------------------------------
    // FLUJO 4 — Borrar usuario → desactiva asignaciones (fan-out + cascada de eventos)
    // ---------------------------------------------------------------------------------------------
    @Test
    @DisplayName("Borrar usuario: desactiva asignaciones (fan-out) Y elimina su perfil (sin huérfanos)")
    void deletingUserDeactivatesAssignmentsAndRemovesProfile() {
        var team = createTeam();
        var backendUserId = onboardMember(team, "Backend Dev", BACKEND_ABILITIES, 40f);
        var taskId = createBackendTask(team, 4);
        assignmentCommandService.handle(new ConfirmRecommendationCommand(taskId, backendUserId));

        // Acción: borrar al usuario → UserDeletedEvent (fan-out: Assignment + Project + Profile).
        userCommandService.handle(new DeleteUserCommand(backendUserId));

        // Automático: las asignaciones del usuario quedan inactivas.
        var assignment = assignmentRepository.findByTaskId(taskId).getFirst();
        assertThat(assignment.isActive()).isFalse();

        // GAP C corregido: el MemberProfile se elimina → ya no queda huérfano ni aparece como candidato.
        assertThat(memberProfileRepository.findByUserId(backendUserId)).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------
    // FLUJO 5 — El alta AUTO-crea un perfil base (evento), pero incompleto no es candidato
    // ---------------------------------------------------------------------------------------------
    @Test
    @DisplayName("SignUp auto-crea perfil base (maxHours=40, sin skills); incompleto → no es candidato")
    void signUpAutoCreatesBaseProfileThatIsNotCandidateUntilSkillsFilled() {
        var team = createTeam();
        var userId = signUpMember(team, uniqueEmail("fresh"), "New Member");

        // Automático por el evento MemberRegistered: el perfil base existe, con maxHours=40 y SIN skills.
        var profile = memberProfileRepository.findByUserId(userId).orElseThrow();
        assertThat(profile.getMaxHours()).isEqualTo(40f);
        assertThat(profile.getEmbeddedAbilities().dimension()).isZero();

        // Incompleto (sin embeddings de skills) → excluido de las recomendaciones.
        var taskId = createBackendTask(team, 4);
        assertThat(topCandidates(taskId, team.getId())).isEmpty();
    }

    // ===================================== Helpers de flujo =======================================

    private Team createTeam() {
        var n = SEQ.incrementAndGet();
        return teamCommandService.handle(new CreateTeamCommand(
                "Team " + n, uniqueEmail("leader"), "Leader " + n, "password123")).orElseThrow();
    }

    private Long signUpMember(Team team, String email, String position) {
        return userCommandService.handle(new SignUpCommand(
                position + " " + SEQ.incrementAndGet(), email, "password123", position, team.getCode()))
                .orElseThrow().getId();
    }

    private Long onboardMember(Team team, String position, String abilities, float maxHours) {
        var userId = signUpMember(team, uniqueEmail(position.replaceAll("\\s", "")), position);
        // El SignUp ya creó un perfil BASE (evento MemberRegistered). El miembro lo COMPLETA con sus
        // skills → ahí se generan los embeddings y pasa a ser candidato.
        memberProfileCommandService.handle(new UpdateMemberProfileCommand(userId, maxHours, abilities, abilities));
        return userId;
    }

    private Long createBackendTask(Team team, int hours) {
        var categoryId = categoryCommandService.handle(new CreateCategoryCommand(
                team.getId(), "Backend " + SEQ.incrementAndGet(), LocalDateTime.now().plusDays(30)))
                .orElseThrow().getId();
        return taskCommandService.handle(new CreateTaskCommand(
                categoryId,
                "Build backend REST API",
                "Implement Spring Boot JPA endpoints and database repositories",
                hours,
                "HIGH",
                "MEDIUM",
                LocalDateTime.now().plusDays(7),
                List.of("Java", "Spring"),
                List.of("REST", "JPA", "backend")
        )).orElseThrow().getId();
    }

    private String uniqueEmail(String prefix) {
        return prefix + SEQ.incrementAndGet() + "@plannia.test";
    }

    // La notificación se envía con @TransactionalEventListener(AFTER_COMMIT) + @Async → corre en otro
    // hilo tras el commit. Hacemos polling hasta verla (o fallamos al agotar el tiempo).
    private void awaitNotificationCount(Long userId, int expected) {
        long deadline = System.currentTimeMillis() + 5000;
        int count = 0;
        while (System.currentTimeMillis() < deadline) {
            count = notificationQueryService.handle(new GetNotificationsByUserIdQuery(userId)).size();
            if (count >= expected) break;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertThat(count).isEqualTo(expected);
    }

    // GetTopCandidates no es @Transactional y carga task.tools/knowledge de forma lazy: en el app web
    // funciona por Open-Session-In-View; en el test lo envolvemos en una transacción para tener sesión.
    private List<CandidateProfile> topCandidates(Long taskId, Long teamId) {
        return new TransactionTemplate(transactionManager).execute(status ->
                assignmentQueryService.handle(new GetTopCandidatesQuery(taskId, teamId)));
    }

    // Vector bag-of-words sobre un vocabulario fijo + dimensión "bias" para que la norma nunca sea 0.
    private static EmbeddingVector deterministicEmbedding(String text) {
        String[] vocab = {
                "backend", "java", "spring", "api", "rest", "jpa", "database", "repositories",
                "frontend", "react", "ui", "css", "design", "figma", "visual", "prototyping",
                "data", "sql", "pipeline", "devops", "docker", "kubernetes", "cloud", "mobile"
        };
        var lower = text.toLowerCase();
        var values = new ArrayList<Float>(vocab.length + 1);
        values.add(1.0f); // bias
        for (var word : vocab) {
            values.add((float) countOccurrences(lower, word));
        }
        return EmbeddingVector.of(values);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
