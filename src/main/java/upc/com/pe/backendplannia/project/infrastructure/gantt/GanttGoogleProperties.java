package upc.com.pe.backendplannia.project.infrastructure.gantt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gantt.google")
public class GanttGoogleProperties {
    private boolean enabled = false;
    private String credentialsJson = "";
    private String templateSheetId = "";
    /** Folder ID inside a Shared Drive where new Gantt copies are created (service accounts have no My Drive quota). */
    private String outputFolderId = "";

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

    public boolean hasTemplateSheet() {
        return templateSheetId != null && !templateSheetId.isBlank();
    }

    public boolean hasOutputFolder() {
        return outputFolderId != null && !outputFolderId.isBlank();
    }
}
