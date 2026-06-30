package upc.com.pe.backendplannia.project.infrastructure.gantt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "gantt.enabled", havingValue = "true")
@EnableConfigurationProperties(GanttGoogleProperties.class)
public class GanttConfiguration {
}
