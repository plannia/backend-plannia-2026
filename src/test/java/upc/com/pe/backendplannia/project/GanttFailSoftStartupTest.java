package upc.com.pe.backendplannia.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import upc.com.pe.backendplannia.profile.application.internal.outboundservices.ai.ProfileEmbeddingService;
import upc.com.pe.backendplannia.project.application.internal.gantt.GanttChartIntegrationException;
import upc.com.pe.backendplannia.project.domain.services.GanttChartPort;
import upc.com.pe.backendplannia.project.infrastructure.gantt.GoogleSheetsGanttAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regresión: con gantt.google.enabled=true pero la integración de Google mal configurada (acá: sin
 * GANTT_OUTPUT_FOLDER_ID), el @PostConstruct del GoogleSheetsGanttAdapter ANTES lanzaba y tumbaba TODO
 * el arranque (la app daba 503). Ahora es fail-soft: la app levanta igual y el Gantt falla solo al
 * usarse (502 vía controller).
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:plannia-gantt;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "JWT_SECRET=integration-test-secret-please-32-chars",
        "notifications.email.transport=log",
        // Escenario del crash: integración Google habilitada pero sin config válida (sin output folder).
        "gantt.enabled=true",
        "gantt.google.enabled=true"
})
class GanttFailSoftStartupTest {

    @MockitoBean
    private ProfileEmbeddingService profileEmbeddingService;

    @Autowired
    private GanttChartPort ganttChartPort;

    @Test
    void contextLoadsEvenWithInvalidGoogleConfig() {
        // Si el test llega hasta acá, el contexto LEVANTÓ pese a enabled=true + config inválida.
        assertThat(ganttChartPort).isInstanceOf(GoogleSheetsGanttAdapter.class);
    }

    @Test
    void ganttFailsGracefullyWhenMisconfigured() {
        // Usar el Gantt mal configurado da un error claro (que el controller traduce a 502), no un crash.
        assertThatThrownBy(() -> ganttChartPort.createSpreadsheet("Demo"))
                .isInstanceOf(GanttChartIntegrationException.class);
    }
}
