package upc.com.pe.backendplannia.project.domain.services;

import upc.com.pe.backendplannia.project.domain.model.readmodels.GanttChartSnapshot;
import upc.com.pe.backendplannia.project.domain.model.readmodels.GanttSpreadsheetResult;

import java.util.List;

public interface GanttChartPort {
    GanttSpreadsheetResult createSpreadsheet(String title);

    void shareWithEmails(String spreadsheetId, List<String> emails);

    void syncContent(String spreadsheetId, GanttChartSnapshot snapshot);
}
