package upc.com.pe.backendplannia.project.infrastructure.gantt;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.project.application.internal.gantt.GanttChartIntegrationException;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Component
public class GanttOAuthFlowService {
    private final GanttGoogleProperties properties;

    GanttOAuthFlowService(GanttGoogleProperties properties) {
        this.properties = properties;
    }

    String buildAuthorizationUrl() {
        ensureWebOAuthConfigured();
        try {
            return flow().newAuthorizationUrl()
                    .setRedirectUri(properties.getOauthRedirectUri())
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .setState(properties.getOauthSetupSecret())
                    .build();
        } catch (IOException | GeneralSecurityException exception) {
            throw new GanttChartIntegrationException("Failed to build Google OAuth URL", exception);
        }
    }

    GoogleTokenResponse exchangeAuthorizationCode(String code) {
        ensureWebOAuthConfigured();
        try {
            return flow().newTokenRequest(code)
                    .setRedirectUri(properties.getOauthRedirectUri())
                    .execute();
        } catch (IOException exception) {
            throw new GanttChartIntegrationException("Failed to exchange OAuth authorization code", exception);
        }
    }

    boolean isWebSetupEnabled() {
        return properties.hasOAuthClientCredentials()
                && properties.hasOauthRedirectUri()
                && properties.hasOauthSetupSecret();
    }

    private GoogleAuthorizationCodeFlow flow() throws IOException, GeneralSecurityException {
        return new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                properties.getOauthClientId(),
                properties.getOauthClientSecret(),
                GanttOAuthScopes.ALL
        ).setAccessType("offline").build();
    }

    private void ensureWebOAuthConfigured() {
        if (!properties.hasOAuthClientCredentials()) {
            throw new GanttChartIntegrationException(
                    "Configure GANTT_OAUTH_CLIENT_ID and GANTT_OAUTH_CLIENT_SECRET.");
        }
        if (!properties.hasOauthRedirectUri()) {
            throw new GanttChartIntegrationException(
                    "Configure GANTT_OAUTH_REDIRECT_URI with your deployed callback URL.");
        }
        if (!properties.hasOauthSetupSecret()) {
            throw new GanttChartIntegrationException(
                    "Configure GANTT_OAUTH_SETUP_SECRET to protect the one-time OAuth setup endpoints.");
        }
    }
}
