package upc.com.pe.backendplannia.project.infrastructure.gantt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gantt.google")
public class GanttGoogleProperties {
    private boolean enabled = false;
    private String credentialsJson = "";
    private String templateSheetId = "";

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

    public boolean hasTemplateSheet() {
        return templateSheetId != null && !templateSheetId.isBlank();
    }
}
