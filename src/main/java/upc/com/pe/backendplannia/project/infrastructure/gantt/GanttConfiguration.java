package upc.com.pe.backendplannia.project.infrastructure.gantt;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GanttGoogleProperties.class)
public class GanttConfiguration {
}
