package upc.com.pe.backendplannia.project.infrastructure.gantt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.project.domain.model.readmodels.GanttChartSnapshot;
import upc.com.pe.backendplannia.project.domain.model.readmodels.GanttSpreadsheetResult;
import upc.com.pe.backendplannia.project.domain.services.GanttChartPort;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(name = "gantt.google.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingGanttChartAdapter implements GanttChartPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingGanttChartAdapter.class);
    private static final AtomicLong COUNTER = new AtomicLong(1);

    @Override
    public GanttSpreadsheetResult createSpreadsheet(String title) {
        var spreadsheetId = "logging-gantt-" + COUNTER.getAndIncrement() + "-" + UUID.randomUUID();
        var spreadsheetUrl = "https://docs.google.com/spreadsheets/d/" + spreadsheetId + "/edit";
        LOGGER.info("[Gantt/log] Created spreadsheet title='{}' id='{}' url='{}'", title, spreadsheetId, spreadsheetUrl);
        return new GanttSpreadsheetResult(spreadsheetId, spreadsheetUrl);
    }

    @Override
    public void shareWithEmails(String spreadsheetId, List<String> emails) {
        LOGGER.info("[Gantt/log] Sharing spreadsheet '{}' with emails: {}", spreadsheetId, emails);
    }

    @Override
    public void syncContent(String spreadsheetId, GanttChartSnapshot snapshot) {
        LOGGER.info(
                "[Gantt/log] Syncing spreadsheet '{}' for category '{}' with {} tasks and {} date columns",
                spreadsheetId,
                snapshot.categoryName(),
                snapshot.taskRows().size(),
                snapshot.dateColumns().size()
        );
    }
}
