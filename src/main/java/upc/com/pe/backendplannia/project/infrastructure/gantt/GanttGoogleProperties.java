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

    public boolean usesServiceAccount() {
        return hasServiceAccountCredentials() && !usesOAuthUser();
    }
}
