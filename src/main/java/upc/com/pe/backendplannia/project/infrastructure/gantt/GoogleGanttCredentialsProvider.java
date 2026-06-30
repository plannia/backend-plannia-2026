package upc.com.pe.backendplannia.project.infrastructure.gantt;

import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.project.application.internal.gantt.GanttChartIntegrationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@ConditionalOnProperty(name = "gantt.enabled", havingValue = "true")
class GoogleGanttCredentialsProvider {
    private static final List<String> SCOPES = List.of(
            SheetsScopes.SPREADSHEETS,
            DriveScopes.DRIVE,
            DriveScopes.DRIVE_FILE
    );

    private final GanttGoogleProperties properties;

    GoogleGanttCredentialsProvider(GanttGoogleProperties properties) {
        this.properties = properties;
    }

    GoogleCredentials build() throws IOException {
        if (properties.usesOAuthUser()) {
            if (properties.getOauthClientId() == null || properties.getOauthClientId().isBlank()
                    || properties.getOauthClientSecret() == null || properties.getOauthClientSecret().isBlank()) {
                throw new GanttChartIntegrationException(
                        "GANTT_OAUTH_CLIENT_ID and GANTT_OAUTH_CLIENT_SECRET are required with GANTT_OAUTH_REFRESH_TOKEN.");
            }
            return UserCredentials.newBuilder()
                    .setClientId(properties.getOauthClientId())
                    .setClientSecret(properties.getOauthClientSecret())
                    .setRefreshToken(properties.getOauthRefreshToken())
                    .build()
                    .createScoped(SCOPES);
        }
        if (properties.hasServiceAccountCredentials()) {
            return GoogleCredentials.fromStream(
                            new ByteArrayInputStream(properties.getCredentialsJson().getBytes(StandardCharsets.UTF_8)))
                    .createScoped(SCOPES);
        }
        throw new GanttChartIntegrationException(
                "Configure Gantt Google auth: for personal Gmail use GANTT_OAUTH_CLIENT_ID, "
                        + "GANTT_OAUTH_CLIENT_SECRET and GANTT_OAUTH_REFRESH_TOKEN; "
                        + "for Workspace Shared Drive use GANTT_GOOGLE_CREDENTIALS_JSON.");
    }
}
