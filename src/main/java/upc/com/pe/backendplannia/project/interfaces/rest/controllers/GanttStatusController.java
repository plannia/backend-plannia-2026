package upc.com.pe.backendplannia.project.interfaces.rest.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import upc.com.pe.backendplannia.project.infrastructure.gantt.GanttGoogleProperties;
import upc.com.pe.backendplannia.project.infrastructure.gantt.GoogleSheetsGanttAdapter;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.GanttStatusResource;

/**
 * Diagnóstico de la integración Gantt (solo si gantt.enabled=true). Autenticado.
 * Sirve para verificar el setup de Google sin leer los logs de Azure.
 */
@RestController
@ConditionalOnProperty(name = "gantt.enabled", havingValue = "true")
@RequestMapping(value = "/api/v1/gantt/status", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Gantt", description = "Gantt integration diagnostics")
public class GanttStatusController {
    private final GanttGoogleProperties properties;
    // null cuando gantt.google.enabled=false (se usa el adapter de log, no el de Google).
    private final GoogleSheetsGanttAdapter googleAdapter;

    public GanttStatusController(
            GanttGoogleProperties properties,
            @Autowired(required = false) GoogleSheetsGanttAdapter googleAdapter
    ) {
        this.properties = properties;
        this.googleAdapter = googleAdapter;
    }

    @GetMapping
    @Operation(summary = "Report Gantt-Google integration readiness and configuration")
    public GanttStatusResource status() {
        var googleMode = googleAdapter != null;
        var ready = googleMode && googleAdapter.isReady();
        var reason = googleMode
                ? googleAdapter.initError()
                : "gantt.google.enabled=false — usando adapter de log (no crea hojas reales de Google).";
        var authMode = properties.usesOAuthUser() ? "oauth-user"
                : properties.usesServiceAccount() ? "service-account"
                : "none";

        return new GanttStatusResource(
                true,
                googleMode ? "google" : "logging",
                ready,
                reason,
                authMode,
                properties.hasOutputFolder(),
                properties.hasOAuthClientCredentials(),
                properties.hasOauthRedirectUri()
        );
    }
}
