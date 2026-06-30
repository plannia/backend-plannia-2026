package upc.com.pe.backendplannia.project.interfaces.rest.controllers;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import upc.com.pe.backendplannia.project.infrastructure.gantt.GanttGoogleProperties;
import upc.com.pe.backendplannia.project.infrastructure.gantt.GanttOAuthFlowService;

import java.io.IOException;

/**
 * One-time OAuth setup for personal Gmail on the deployed backend.
 * After obtaining the refresh token, add it to Azure as GANTT_OAUTH_REFRESH_TOKEN.
 */
@RestController
@RequestMapping("/api/v1/gantt/oauth")
public class GanttOAuthSetupController {
    private final GanttGoogleProperties properties;
    private final GanttOAuthFlowService oauthFlowService;

    public GanttOAuthSetupController(GanttGoogleProperties properties, GanttOAuthFlowService oauthFlowService) {
        this.properties = properties;
        this.oauthFlowService = oauthFlowService;
    }

    @GetMapping("/authorize")
    public void authorize(@RequestParam String secret, HttpServletResponse response) throws IOException {
        rejectIfSetupDisabled();
        rejectIfSecretInvalid(secret);
        response.sendRedirect(oauthFlowService.buildAuthorizationUrl());
    }

    @GetMapping(value = "/callback", produces = MediaType.TEXT_HTML_VALUE)
    public String callback(
            @RequestParam String code,
            @RequestParam(required = false) String state
    ) {
        rejectIfSetupDisabled();
        if (state == null || !properties.getOauthSetupSecret().equals(state)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid OAuth state");
        }

        var tokenResponse = oauthFlowService.exchangeAuthorizationCode(code);
        var refreshToken = tokenResponse.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            return htmlPage(
                    "No se recibió refresh token",
                    """
                    <p>Revoca el acceso de Plannia en \
                    <a href="https://myaccount.google.com/permissions">Google Account Permissions</a> \
                    y vuelve a autorizar.</p>
                    """
            );
        }

        return htmlPage(
                "OAuth listo — copia el refresh token en Azure",
                """
                <p>Agrega estas App Settings en Azure y reinicia el backend:</p>
                <pre>GANTT_OAUTH_REFRESH_TOKEN=%s</pre>
                <pre>GANTT_GOOGLE_ENABLED=true</pre>
                <pre>GANTT_OUTPUT_FOLDER_ID=&lt;id de tu carpeta plannia en Drive&gt;</pre>
                <p>Luego prueba <strong>Generar Gantt</strong> en la app desplegada.</p>
                """.formatted(escapeHtml(refreshToken))
        );
    }

    private void rejectIfSetupDisabled() {
        if (!oauthFlowService.isWebSetupEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gantt OAuth web setup is not configured"
            );
        }
    }

    private void rejectIfSecretInvalid(String secret) {
        if (!properties.getOauthSetupSecret().equals(secret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid setup secret");
        }
    }

    private static String htmlPage(String title, String body) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <title>%s</title>
                  <style>
                    body { font-family: system-ui, sans-serif; max-width: 720px; margin: 40px auto; padding: 0 16px; }
                    pre { background: #111827; color: #f9fafb; padding: 12px; overflow-x: auto; border-radius: 8px; }
                  </style>
                </head>
                <body>
                  <h1>%s</h1>
                  %s
                </body>
                </html>
                """.formatted(escapeHtml(title), escapeHtml(title), body);
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
