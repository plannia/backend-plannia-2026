package upc.com.pe.backendplannia.project.infrastructure.gantt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gantt.google")
public class GanttGoogleProperties {
    private boolean enabled = false;
    private String credentialsJson = "";
    private String templateSheetId = "";
    /** Folder ID where new Gantt spreadsheets are created. */
    private String outputFolderId = "";
    /** OAuth client ID (Desktop/Web app) for personal Gmail. */
    private String oauthClientId = "";
    private String oauthClientSecret = "";
    private String oauthRefreshToken = "";
    /** Callback URL registered in Google Cloud (Web OAuth client), e.g. https://your-api.azurewebsites.net/api/v1/gantt/oauth/callback */
    private String oauthRedirectUri = "";
    /** Secret required to open /api/v1/gantt/oauth/authorize (one-time setup). */
    private String oauthSetupSecret = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCredentialsJson() {
        return credentialsJson;
    }

    public void setCredentialsJson(String credentialsJson) {
        this.credentialsJson = credentialsJson;
    }

    public String getTemplateSheetId() {
        return templateSheetId;
    }

    public void setTemplateSheetId(String templateSheetId) {
        this.templateSheetId = templateSheetId;
    }

    public String getOutputFolderId() {
        return outputFolderId;
    }

    public void setOutputFolderId(String outputFolderId) {
        this.outputFolderId = outputFolderId;
    }

    public String getOauthClientId() {
        return oauthClientId;
    }

    public void setOauthClientId(String oauthClientId) {
        this.oauthClientId = oauthClientId;
    }

    public String getOauthClientSecret() {
        return oauthClientSecret;
    }

    public void setOauthClientSecret(String oauthClientSecret) {
        this.oauthClientSecret = oauthClientSecret;
    }

    public String getOauthRefreshToken() {
        return oauthRefreshToken;
    }

    public void setOauthRefreshToken(String oauthRefreshToken) {
        this.oauthRefreshToken = oauthRefreshToken;
    }

    public String getOauthRedirectUri() {
        return oauthRedirectUri;
    }

    public void setOauthRedirectUri(String oauthRedirectUri) {
        this.oauthRedirectUri = oauthRedirectUri;
    }

    public String getOauthSetupSecret() {
        return oauthSetupSecret;
    }

    public void setOauthSetupSecret(String oauthSetupSecret) {
        this.oauthSetupSecret = oauthSetupSecret;
    }

    public boolean hasTemplateSheet() {
        return templateSheetId != null && !templateSheetId.isBlank();
    }

    public boolean hasOutputFolder() {
        return outputFolderId != null && !outputFolderId.isBlank();
    }

    public boolean hasServiceAccountCredentials() {
        return credentialsJson != null && !credentialsJson.isBlank();
    }

    public boolean usesOAuthUser() {
        return oauthRefreshToken != null && !oauthRefreshToken.isBlank();
    }

    public boolean hasOAuthClientCredentials() {
        return oauthClientId != null && !oauthClientId.isBlank()
                && oauthClientSecret != null && !oauthClientSecret.isBlank();
    }

    public boolean hasOauthRedirectUri() {
        return oauthRedirectUri != null && !oauthRedirectUri.isBlank();
    }

    public boolean hasOauthSetupSecret() {
        return oauthSetupSecret != null && !oauthSetupSecret.isBlank();
    }

    public boolean usesServiceAccount() {
        return hasServiceAccountCredentials() && !usesOAuthUser();
    }
}
