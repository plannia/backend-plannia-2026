package upc.com.pe.backendplannia.assignment.application.internal;

import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de COSTO/CARGA contra Azure OpenAI (text-embedding-3-large): hace ~50 llamadas de distintos
 * tipos, suma los tokens REALES que reporta la API y estima el gasto. Sirve para medir el impacto en
 * los créditos. NO corre en la suite normal: protegido por AZURE_EMBEDDING_LOAD_TEST=true.
 *
 * Lee base-url/api-key/model de application-dev.properties (gitignored).
 */
@SpringJUnitConfig
@ContextConfiguration(classes = AzureEmbeddingCostTest.EmbeddingModelTestConfiguration.class)
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-dev.properties"})
@EnabledIfEnvironmentVariable(named = "AZURE_EMBEDDING_LOAD_TEST", matches = "true")
class AzureEmbeddingCostTest {
    private static final Logger log = LoggerFactory.getLogger(AzureEmbeddingCostTest.class);

    // Precio de text-embedding-3-large: USD 0.13 por 1,000,000 tokens.
    private static final double PRICE_PER_MILLION_USD = 0.13;

    private static long totalCalls = 0;
    private static long totalApiRequests = 0; // peticiones HTTP reales (un batch = 1 request)
    private static long totalTokens = 0;

    @Autowired
    private EmbeddingModel embeddingModel;

    // ---- Tipo 1: muchas descripciones individuales y variadas (1 embedding por llamada) ----
    @Test
    void embedsAWideVarietyOfTexts() {
        var corpus = List.of(
                "Build Spring Boot REST endpoints with JPA repositories and PostgreSQL",
                "Design a responsive landing page in Figma with a cohesive visual system",
                "Implement JWT authentication and role-based authorization in a backend API",
                "Create a CI/CD pipeline with GitHub Actions to deploy to Azure App Service",
                "Develop a React component library with reusable hooks and unit tests",
                "Set up a Kafka consumer to process streaming events and persist them",
                "Optimize slow SQL queries and add appropriate database indexes",
                "Train a recommendation model using embeddings and cosine similarity",
                "Write end-to-end tests with Cypress for the checkout flow",
                "Configure Kubernetes deployments, services and horizontal pod autoscaling",
                "Build a mobile app screen in Flutter with state management",
                "Implement caching with Redis to reduce API latency",
                "Create a data pipeline in Python to clean and aggregate CSV files",
                "Design accessible UI components following WCAG guidelines",
                "Set up monitoring and alerting with Prometheus and Grafana",
                "Refactor a legacy monolith into modular bounded contexts",
                "Implement a payment integration with Stripe webhooks",
                "Write Terraform modules to provision cloud infrastructure",
                "Build a GraphQL API with pagination and error handling",
                "Develop an Android push notification feature with FCM",
                "Backend developer skilled in Java, Spring Boot, Hibernate and REST APIs",
                "UX designer focused on wireframes, prototyping and visual design systems",
                "Data engineer experienced with Spark, ETL pipelines and SQL warehouses",
                "DevOps engineer with Docker, Kubernetes, Terraform and CI/CD expertise",
                "Frontend developer specialized in React, TypeScript and responsive layouts",
                "QA engineer experienced in automated testing, Cypress and Selenium",
                "Machine learning engineer working with embeddings, PyTorch and model serving",
                "Security engineer focused on authentication, OAuth and penetration testing",
                "Mobile developer with Flutter, Kotlin and cross-platform experience",
                "Product manager coordinating roadmaps, user stories and stakeholder alignment",
                "Diseñar el flujo de onboarding de usuarios nuevos en la aplicación móvil",
                "Implementar el módulo de reportes con gráficos y exportación a PDF",
                "Integrar inicio de sesión con Google y Microsoft mediante OAuth2",
                "Crear pruebas unitarias y de integración para el servicio de pagos",
                "Mejorar el rendimiento de la API reduciendo consultas N+1 a la base de datos",
                "Configurar el despliegue automático del backend en la nube",
                "Desarrollar la pantalla de perfil del usuario con edición en línea",
                "Modelar el dominio de asignaciones siguiendo principios de DDD",
                "Documentar los endpoints con OpenAPI y ejemplos de peticiones",
                "Añadir notificaciones por correo cuando se asigna una tarea a un miembro"
        );

        for (var text : corpus) {
            embed(text);
        }
        log.info("Tipo 1 (individuales): {} textos embebidos", corpus.size());
        assertThat(corpus).hasSizeGreaterThan(30);
    }

    // ---- Tipo 2: batch (varios inputs en UNA sola petición HTTP) ----
    @Test
    void embedsABatchInASingleRequest() {
        var batch = List.of(
                "Implement OAuth2 login", "Design the dashboard layout", "Write integration tests",
                "Provision infrastructure with Terraform", "Build the notifications service",
                "Optimize database indexes", "Create the mobile profile screen",
                "Set up Prometheus monitoring", "Develop a GraphQL API", "Train an embedding model"
        );
        var before = totalTokens;
        embedBatch(batch);
        log.info("Tipo 2 (batch): {} inputs en 1 petición → {} tokens", batch.size(), totalTokens - before);
        assertThat(batch).hasSize(10);
    }

    // ---- Tipo 3: escenarios de ranking reales (tarea + 3 perfiles → similitud coseno) ----
    @Test
    void runsRealRankingScenarios() {
        rankingScenario(
                "Build Spring Boot REST endpoints with JPA repositories and backend tests",
                List.of(
                        "Spring Boot REST APIs, Java controllers, services and JPA repositories",
                        "Figma wireframes, CSS layouts and visual design systems",
                        "Spark ETL pipelines, SQL warehouses and data modeling"
                ),
                0 // se espera que gane el candidato backend (índice 0)
        );
        rankingScenario(
                "Design a polished responsive UI mockup in Figma with branding and animations",
                List.of(
                        "Spring Boot REST APIs, Java controllers and JPA repositories",
                        "Figma wireframes, CSS layouts, visual design systems and animations",
                        "Kubernetes, Docker and CI/CD pipelines"
                ),
                1 // se espera que gane el candidato de diseño (índice 1)
        );
        rankingScenario(
                "Set up Kubernetes deployments, Docker images and a CI/CD pipeline to the cloud",
                List.of(
                        "React, TypeScript and responsive frontend layouts",
                        "Product roadmaps, user stories and stakeholder alignment",
                        "Kubernetes, Docker, Terraform and CI/CD automation"
                ),
                2 // se espera que gane el candidato devops (índice 2)
        );
    }

    private void rankingScenario(String taskText, List<String> candidateTexts, int expectedWinnerIndex) {
        var taskEmbedding = embed(taskText);
        var scores = new ArrayList<Float>();
        for (var candidateText : candidateTexts) {
            scores.add(embed(candidateText).cosineSimilarity(taskEmbedding));
        }
        int winner = 0;
        for (int i = 1; i < scores.size(); i++) {
            if (scores.get(i) > scores.get(winner)) winner = i;
        }
        log.info("Ranking | tarea=\"{}\" | scores={} | ganador=índice {}", taskText, scores, winner);
        assertThat(winner).isEqualTo(expectedWinnerIndex);
    }

    private EmbeddingVector embed(String text) {
        var response = embeddingModel.call(new EmbeddingRequest(List.of(text), null));
        accumulate(response.getMetadata().getUsage().getTotalTokens(), response.getResults().size());
        float[] vector = response.getResults().getFirst().getOutput();
        var values = new ArrayList<Float>(vector.length);
        for (float v : vector) values.add(v);
        return EmbeddingVector.of(values);
    }

    private void embedBatch(List<String> texts) {
        var response = embeddingModel.call(new EmbeddingRequest(texts, null));
        accumulate(response.getMetadata().getUsage().getTotalTokens(), response.getResults().size());
    }

    private static synchronized void accumulate(Integer tokens, int embeddingsInResponse) {
        totalApiRequests += 1;
        totalCalls += embeddingsInResponse;
        totalTokens += (tokens != null ? tokens : 0);
    }

    @AfterAll
    static void printCostSummary() {
        double runCost = (totalTokens / 1_000_000.0) * PRICE_PER_MILLION_USD;
        double avgTokens = totalCalls == 0 ? 0 : (double) totalTokens / totalCalls;
        log.info("");
        log.info("===================== RESUMEN DE COSTO (text-embedding-3-large) =====================");
        log.info(" Peticiones HTTP a Azure : {}", totalApiRequests);
        log.info(" Embeddings generados    : {}", totalCalls);
        log.info(" Tokens totales (reales) : {}", totalTokens);
        log.info(" Promedio tokens/embedding: {}", String.format(java.util.Locale.US, "%.1f", avgTokens));
        log.info(" Precio                  : USD {} / 1,000,000 tokens", PRICE_PER_MILLION_USD);
        log.info(" -----------------------------------------------------------------------------------");
        log.info(" COSTO DE ESTA CORRIDA   : USD {}", String.format(java.util.Locale.US, "%.6f", runCost));
        log.info(" Extrapolación x100      : USD {}", String.format(java.util.Locale.US, "%.4f", runCost * 100));
        log.info(" Extrapolación x1,000    : USD {}", String.format(java.util.Locale.US, "%.4f", runCost * 1000));
        log.info(" Cuántas corridas con $5 : {}", runCost == 0 ? "∞" : String.format(java.util.Locale.US, "%,.0f", 5.0 / runCost));
        log.info("====================================================================================");
    }

    @TestConfiguration
    static class EmbeddingModelTestConfiguration {
        @Bean
        EmbeddingModel embeddingModel(Environment environment) {
            var baseUrl = normalizeBaseUrl(environment.getProperty("spring.ai.openai.base-url", "http://127.0.0.1:1234"));
            var apiKey = environment.getProperty("spring.ai.openai.api-key", "lm-studio");
            var model = environment.getProperty("spring.ai.openai.embedding.options.model", "text-embedding-3-large");
            log.info("Cliente de embeddings | baseUrl={} | model={}", baseUrl, model);

            var client = OpenAIOkHttpClient.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .maxRetries(0)
                    .build();
            var options = OpenAiEmbeddingOptions.builder().model(model).build();
            return new OpenAiEmbeddingModel(client, MetadataMode.EMBED, options);
        }

        private String normalizeBaseUrl(String baseUrl) {
            var normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return normalized.endsWith("/v1") ? normalized : normalized + "/v1";
        }
    }
}
