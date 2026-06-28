package upc.com.pe.backendplannia.project.application.internal.schedulers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.project.application.internal.gantt.GanttChartIntegrationException;
import upc.com.pe.backendplannia.project.domain.model.commands.SyncCategoryGanttCommand;
import upc.com.pe.backendplannia.project.domain.services.GanttChartCommandService;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories.CategoryRepository;

@Component
public class GanttSyncScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GanttSyncScheduler.class);

    private final CategoryRepository categoryRepository;
    private final GanttChartCommandService ganttChartCommandService;

    public GanttSyncScheduler(
            CategoryRepository categoryRepository,
            GanttChartCommandService ganttChartCommandService
    ) {
        this.categoryRepository = categoryRepository;
        this.ganttChartCommandService = ganttChartCommandService;
    }

    @Scheduled(cron = "${gantt.sync.cron:0 0 12 * * *}", zone = "${gantt.sync.zone:America/Lima}")
    public void syncAllGanttCharts() {
        var categories = categoryRepository.findByGanttSpreadsheetIdIsNotNull();
        LOGGER.info("Starting daily Gantt sync for {} categories", categories.size());

        categories.forEach(category -> {
            try {
                ganttChartCommandService.handle(new SyncCategoryGanttCommand(category.getId()));
                LOGGER.info("Gantt sync completed for category {}", category.getId());
            } catch (GanttChartIntegrationException exception) {
                LOGGER.error("Gantt sync failed for category {}: {}", category.getId(), exception.getMessage());
            } catch (RuntimeException exception) {
                LOGGER.error("Unexpected Gantt sync failure for category {}", category.getId(), exception);
            }
        });
    }
}
