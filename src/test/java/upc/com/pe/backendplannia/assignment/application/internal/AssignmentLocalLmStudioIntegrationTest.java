package upc.com.pe.backendplannia.assignment.application.internal;

import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import upc.com.pe.backendplannia.assignment.application.internal.outboundservices.TaskRequirementGateway;
import upc.com.pe.backendplannia.assignment.application.internal.queryservices.AssignmentQueryServiceImpl;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetTopCandidatesQuery;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.ScoredCandidate;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.TaskRequirement;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentQueryService;
import upc.com.pe.backendplannia.assignment.domain.services.CandidateProfileProvider;
import upc.com.pe.backendplannia.assignment.domain.services.ScoringDomainService;
import upc.com.pe.backendplannia.assignment.domain.services.TaskRequirementResolver;
import upc.com.pe.backendplannia.assignment.infrastructure.persistence.jpa.repositories.AssignmentRepository;
import upc.com.pe.backendplannia.profile.application.internal.outboundservices.ai.ProfileEmbeddingService;
import upc.com.pe.backendplannia.profile.infrastructure.ai.OpenAiProfileEmbeddingService;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
@ContextConfiguration(classes = {
        AssignmentQueryServiceImpl.class,
        ScoringDomainService.class,
        TaskRequirementGateway.class,
        AssignmentLocalLmStudioIntegrationTest.LocalLmStudioEmbeddingTestConfiguration.class
})
// application-dev.properties (gitignored, segundo → gana) aporta la base-url y el token reales de tu
// LM Studio local (spring.ai.openai.base-url / api-key). También se pueden overridear con las
// system properties/env vars LMSTUDIO_BASE_URL / LMSTUDIO_API_KEY, que tienen prioridad.
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-dev.properties"})
@EnabledIfEnvironmentVariable(named = "LMSTUDIO_EMBEDDING_TEST", matches = "true")
class AssignmentLocalLmStudioIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(AssignmentLocalLmStudioIntegrationTest.class);

    private static final Long TASK_ID = 701L;
    private static final Long TEAM_ID = 301L;
    private static final Long BEST_USER_ID = 101L;
    private static final Long SECOND_USER_ID = 102L;
    private static final Long THIRD_USER_ID = 103L;

    @Autowired
    private AssignmentQueryService assignmentQueryService;

    @Autowired
    private ProfileEmbeddingService profileEmbeddingService;

    @Autowired
    private ScoringDomainService scoringDomainService;

    @MockitoBean
    private AssignmentRepository assignmentRepository;

    @MockitoBean
    private CandidateProfileProvider candidateProfileProvider;

    @MockitoBean
    private TaskRequirementResolver taskRequirementResolver;

    @Test
    void getTopCandidatesUsesLmStudioEmbeddingsAndRanksBestBackendCandidateFirst() {
        log.info("Starting LM Studio ranking integration test");
        var task = mockTask("Build Spring Boot REST endpoints for assignment recommendations using JPA repositories");
        var taskRequirement = taskRequirement(task);
        var bestCandidate = candidate(
                BEST_USER_ID,
                "Spring Boot REST APIs, Java controllers, services, JPA repositories and backend testing",
                "Implemented production REST endpoints with Spring Boot, Hibernate and PostgreSQL",
                "Backend architecture, API design and service-layer implementation",
                2f,
                8f
        );
        var secondCandidate = candidate(
                SECOND_USER_ID,
                "Java backend development, SQL databases and simple service integrations",
                "Maintained backend services and created database queries for internal systems",
                "Backend development and data modeling",
                1f,
                8f
        );
        var thirdCandidate = candidate(
                THIRD_USER_ID,
                "Figma wireframes, CSS layouts, visual design systems and frontend animations",
                "Designed web mockups and improved responsive user interfaces",
                "Product design, branding and UI prototyping",
                0f,
                8f
        );
        var candidates = List.of(thirdCandidate, secondCandidate, bestCandidate);

        logTaskRequirement(task, taskRequirement);
        logCandidateScores(candidates, taskRequirement);

        when(taskRequirementResolver.resolveByTaskId(TASK_ID)).thenReturn(Optional.of(taskRequirement));
        when(candidateProfileProvider.findByTeamId(TEAM_ID)).thenReturn(candidates);

        var result = assignmentQueryService.handle(new GetTopCandidatesQuery(TASK_ID, TEAM_ID));

        logRanking(result, taskRequirement);
        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting(scored -> scored.candidate().userId())
                .containsExactly(BEST_USER_ID, SECOND_USER_ID, THIRD_USER_ID);
    }

    // Verifica que el ranking depende de la TAREA: con los MISMOS candidatos pero una tarea de diseño,
    // el orden se invierte y la diseñadora pasa a primera. Prueba que el embedding de la tarea manda.
    @Test
    void getTopCandidatesRanksDesignerFirstForDesignTask() {
        log.info("Starting LM Studio task-driven ranking test (design task)");
        var task = mockTask("Design a polished, responsive UI mockup in Figma with a cohesive visual "
                + "design system, branding and animations for the product landing page");
        var taskRequirement = taskRequirement(task);

        var backendExpert = candidate(
                BEST_USER_ID,
                "Spring Boot REST APIs, Java controllers, services, JPA repositories and backend testing",
                "Implemented production REST endpoints with Spring Boot, Hibernate and PostgreSQL",
                "Backend architecture, API design and service-layer implementation",
                2f,
                8f
        );
        var designer = candidate(
                THIRD_USER_ID,
                "Figma wireframes, CSS layouts, visual design systems and frontend animations",
                "Designed web mockups and improved responsive user interfaces",
                "Product design, branding and UI prototyping",
                0f,
                8f
        );
        var candidates = List.of(backendExpert, designer);

        logCandidateScores(candidates, taskRequirement);
        when(taskRequirementResolver.resolveByTaskId(TASK_ID)).thenReturn(Optional.of(taskRequirement));
        when(candidateProfileProvider.findByTeamId(TEAM_ID)).thenReturn(candidates);

        var result = assignmentQueryService.handle(new GetTopCandidatesQuery(TASK_ID, TEAM_ID));

        logRanking(result, taskRequirement);
        assertThat(result)
                .extracting(scored -> scored.candidate().userId())
                .containsExactly(THIRD_USER_ID, BEST_USER_ID);
    }

    // Verifica que el ranking depende de las VARIABLES DE PERFIL (carga): el candidato con mejor skill
    // pero SIN horas disponibles (8/8 ocupadas) se excluye, y gana uno disponible aunque sea menos experto.
    @Test
    void getTopCandidatesExcludesOverloadedCandidateRegardlessOfSkill() {
        log.info("Starting LM Studio availability-filtering test");
        var task = mockTask(
                "Build Spring Boot REST endpoints with JPA repositories and backend tests",
                6
        );
        var taskRequirement = taskRequirement(task);

        var overloadedExpert = candidate(
                BEST_USER_ID,
                "Spring Boot REST APIs, Java controllers, services, JPA repositories and backend testing",
                "Implemented production REST endpoints with Spring Boot, Hibernate and PostgreSQL",
                "Backend architecture, API design and service-layer implementation",
                8f,   // 8 horas activas de 8 → availableHours = 0 < 6 estimadas → excluido
                8f
        );
        var availableJunior = candidate(
                SECOND_USER_ID,
                "Java backend development, SQL databases and simple service integrations",
                "Maintained backend services and created database queries for internal systems",
                "Backend development and data modeling",
                0f,   // 0 horas activas de 8 → availableHours = 8 >= 6 → disponible
                8f
        );
        var candidates = List.of(overloadedExpert, availableJunior);

        logCandidateScores(candidates, taskRequirement);
        when(taskRequirementResolver.resolveByTaskId(TASK_ID)).thenReturn(Optional.of(taskRequirement));
        when(candidateProfileProvider.findByTeamId(TEAM_ID)).thenReturn(candidates);

        var result = assignmentQueryService.handle(new GetTopCandidatesQuery(TASK_ID, TEAM_ID));

        logRanking(result, taskRequirement);
        // El experto sobrecargado NO aparece pese a tener el mejor match de skill; gana el disponible.
        assertThat(result)
                .extracting(scored -> scored.candidate().userId())
                .containsExactly(SECOND_USER_ID);
    }

    private MockTask mockTask(String description) {
        return mockTask(description, 4);
    }

    private MockTask mockTask(String description, int estimatedHours) {
        log.info("Generating task embedding | taskId={} | description=\"{}\"", TASK_ID, description);
        var embedding = profileEmbeddingService.generateEmbedding(description);
        log.info(
                "Generated task embedding | taskId={} | dimension={} | preview={}",
                TASK_ID,
                embedding.dimension(),
                embeddingPreview(embedding)
        );
        return new MockTask(
                TASK_ID,
                description,
                embedding,
                estimatedHours,
                "HIGH",
                "MEDIUM"
        );
    }

    private TaskRequirement taskRequirement(MockTask task) {
        assertThat(task.description()).isNotBlank();
        return new TaskRequirement(
                task.taskId(),
                task.embeddedDescription(),
                task.estimatedHours(),
                task.urgency(),
                task.difficulty()
        );
    }

    private CandidateProfile candidate(
            Long userId,
            String abilities,
            String experience,
            String interests,
            float activeHours,
            float maxHours
    ) {
        log.info("Generating profile embeddings | userId={}", userId);
        var embeddedAbilities = profileEmbeddingService.generateEmbedding(abilities);
        var embeddedExperience = profileEmbeddingService.generateEmbedding(experience);
        var embeddedInterests = profileEmbeddingService.generateEmbedding(interests);
        log.info(
                "Generated profile embeddings | userId={} | abilitiesDim={} | experienceDim={} | interestsDim={}",
                userId,
                embeddedAbilities.dimension(),
                embeddedExperience.dimension(),
                embeddedInterests.dimension()
        );
        return new CandidateProfile(
                userId,
                embeddedAbilities,
                embeddedExperience,
                embeddedInterests,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                activeHours,
                maxHours
        );
    }

    private void logTaskRequirement(MockTask task, TaskRequirement taskRequirement) {
        log.info(
                "Task requirement | taskId={} | estimatedHours={} | urgency={} | difficulty={} | description=\"{}\" | embeddingDim={}",
                task.taskId(),
                taskRequirement.estimatedHours(),
                taskRequirement.urgency(),
                taskRequirement.difficulty(),
                task.description(),
                taskRequirement.requirementsEmbedding().dimension()
        );
    }

    private void logCandidateScores(List<CandidateProfile> candidates, TaskRequirement taskRequirement) {
        log.info("Candidate score breakdown before ranking");
        for (var candidate : candidates) {
            var skillMatch = scoringDomainService.calculateSkillMatch(candidate, taskRequirement);
            var experienceMatch = scoringDomainService.calculateExperienceMatch(candidate, taskRequirement);
            var interestMatch = scoringDomainService.calculateInterestMatch(candidate, taskRequirement);
            var score = scoringDomainService.calculateScore(candidate, taskRequirement);
            var available = scoringDomainService.meetsAvailabilityThreshold(candidate, taskRequirement);

            log.info(
                    "Candidate score | userId={} | activeHours={} | maxHours={} | availableHours={} | available={} | skillMatch={} | experienceMatch={} | interestMatch={} | score={}",
                    candidate.userId(),
                    candidate.activeHours(),
                    candidate.maxHours(),
                    candidate.availableHours(),
                    available,
                    formatScore(skillMatch),
                    formatScore(experienceMatch),
                    formatScore(interestMatch),
                    formatScore(score)
            );
        }
    }

    private void logRanking(List<ScoredCandidate> ranking, TaskRequirement taskRequirement) {
        log.info("Final ranking returned by AssignmentQueryService");
        for (int i = 0; i < ranking.size(); i++) {
            var scored = ranking.get(i);
            log.info(
                    "Ranking position {} | userId={} | score={}",
                    i + 1,
                    scored.candidate().userId(),
                    formatScore(scored.totalScore())
            );
        }
    }

    private String embeddingPreview(EmbeddingVector embedding) {
        return embedding.values().stream()
                .limit(5)
                .map(this::formatScore)
                .toList()
                .toString();
    }

    private String formatScore(float value) {
        return String.format(java.util.Locale.US, "%.6f", value);
    }

    private record MockTask(
            Long taskId,
            String description,
            EmbeddingVector embeddedDescription,
            int estimatedHours,
            String urgency,
            String difficulty
    ) {
    }

    @TestConfiguration
    static class LocalLmStudioEmbeddingTestConfiguration {
        @Bean
        ProfileEmbeddingService profileEmbeddingService(Environment environment) {
            var baseUrl = normalizeOpenAiBaseUrl(configurationValue(
                    environment,
                    "LMSTUDIO_BASE_URL",
                    "spring.ai.openai.base-url",
                    "http://127.0.0.1:1234/v1"
            ));
            var apiKey = configurationValue(
                    environment,
                    "LMSTUDIO_API_KEY",
                    "spring.ai.openai.api-key",
                    "lm-studio"
            );
            var model = configurationValue(
                    environment,
                    "LMSTUDIO_EMBEDDING_MODEL",
                    "spring.ai.openai.embedding.options.model",
                    "nomic-embed-text"
            );
            log.info(
                    "Configuring LM Studio embedding client | baseUrl={} | model={} | apiKey={}",
                    baseUrl,
                    model,
                    redact(apiKey)
            );

            var openAiClient = OpenAIOkHttpClient.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .maxRetries(0)
                    .build();

            var optionsBuilder = OpenAiEmbeddingOptions.builder();
            optionsBuilder.model(model);
            var embeddingModel = new OpenAiEmbeddingModel(
                    openAiClient,
                    MetadataMode.EMBED,
                    optionsBuilder.build()
            );

            return new OpenAiProfileEmbeddingService(embeddingModel);
        }

        private String configurationValue(
                Environment environment,
                String key,
                String propertyKey,
                String defaultValue
        ) {
            var systemProperty = System.getProperty(key);
            if (systemProperty != null && !systemProperty.isBlank()) {
                return systemProperty;
            }
            var environmentValue = System.getenv(key);
            if (environmentValue != null && !environmentValue.isBlank()) {
                return environmentValue;
            }
            var propertyValue = environment.getProperty(propertyKey);
            if (propertyValue != null && !propertyValue.isBlank()) {
                return propertyValue;
            }
            return defaultValue;
        }

        private String normalizeOpenAiBaseUrl(String baseUrl) {
            var normalizedBaseUrl = baseUrl.endsWith("/")
                    ? baseUrl.substring(0, baseUrl.length() - 1)
                    : baseUrl;
            if (normalizedBaseUrl.endsWith("/v1")) {
                return normalizedBaseUrl;
            }
            return normalizedBaseUrl + "/v1";
        }

        private String redact(String value) {
            if (value == null || value.isBlank()) {
                return "<empty>";
            }
            if (value.length() <= 8) {
                return "****";
            }
            return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
        }
    }
}
