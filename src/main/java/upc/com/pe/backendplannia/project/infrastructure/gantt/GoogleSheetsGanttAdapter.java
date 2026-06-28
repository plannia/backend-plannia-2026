package upc.com.pe.backendplannia.project.infrastructure.gantt;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.DimensionProperties;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.google.api.services.sheets.v4.model.UpdateDimensionPropertiesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.project.application.internal.gantt.GanttChartIntegrationException;
import upc.com.pe.backendplannia.project.application.internal.gantt.GanttChartDataBuilder;
import upc.com.pe.backendplannia.project.domain.model.readmodels.GanttChartSnapshot;
import upc.com.pe.backendplannia.project.domain.model.readmodels.GanttSpreadsheetResult;
import upc.com.pe.backendplannia.project.domain.model.readmodels.GanttTaskRow;
import upc.com.pe.backendplannia.project.domain.services.GanttChartPort;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "gantt.google.enabled", havingValue = "true")
public class GoogleSheetsGanttAdapter implements GanttChartPort {
    private static final String APPLICATION_NAME = "Plannia";
    private static final int FIXED_COLUMNS = 5;
    private static final DateTimeFormatter DATE_HEADER_FORMAT = DateTimeFormatter.ofPattern("MMM d", Locale.US);
    private static final DateTimeFormatter TASK_DATE_FORMAT = DateTimeFormatter.ofPattern("M/d/yy");

    private final GanttGoogleProperties properties;
    private Sheets sheetsService;
    private Drive driveService;

    public GoogleSheetsGanttAdapter(GanttGoogleProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        if (properties.getCredentialsJson() == null || properties.getCredentialsJson().isBlank()) {
            throw new GanttChartIntegrationException("gantt.google.credentials-json is required when gantt.google.enabled=true");
        }

        try {
            var credentials = GoogleCredentials.fromStream(
                            new ByteArrayInputStream(properties.getCredentialsJson().getBytes(StandardCharsets.UTF_8)))
                    .createScoped(List.of(
                            SheetsScopes.SPREADSHEETS,
                            DriveScopes.DRIVE,
                            DriveScopes.DRIVE_FILE
                    ));

            var requestInitializer = new HttpCredentialsAdapter(credentials);
            var transport = GoogleNetHttpTransport.newTrustedTransport();
            var jsonFactory = GsonFactory.getDefaultInstance();

            sheetsService = new Sheets.Builder(transport, jsonFactory, requestInitializer)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            driveService = new Drive.Builder(transport, jsonFactory, requestInitializer)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (IOException | GeneralSecurityException exception) {
            throw new GanttChartIntegrationException("Failed to initialize Google Sheets client", exception);
        }
    }

    @Override
    public GanttSpreadsheetResult createSpreadsheet(String title) {
        try {
            if (properties.hasTemplateSheet()) {
                var copied = driveService.files()
                        .copy(properties.getTemplateSheetId(), new File().setName(title))
                        .setSupportsAllDrives(true)
                        .execute();
                var spreadsheetId = copied.getId();
                var spreadsheetUrl = "https://docs.google.com/spreadsheets/d/" + spreadsheetId + "/edit";
                return new GanttSpreadsheetResult(spreadsheetId, spreadsheetUrl);
            }

            var spreadsheet = new Spreadsheet()
                    .setProperties(new SpreadsheetProperties().setTitle(title));
            var created = sheetsService.spreadsheets().create(spreadsheet).execute();
            var spreadsheetId = created.getSpreadsheetId();
            var spreadsheetUrl = "https://docs.google.com/spreadsheets/d/" + spreadsheetId + "/edit";
            return new GanttSpreadsheetResult(spreadsheetId, spreadsheetUrl);
        } catch (IOException exception) {
            throw new GanttChartIntegrationException("Failed to create Google Spreadsheet", exception);
        }
    }

    @Override
    public void shareWithEmails(String spreadsheetId, List<String> emails) {
        for (var email : emails) {
            try {
                var permission = new Permission()
                        .setType("user")
                        .setRole("writer")
                        .setEmailAddress(email);
                driveService.permissions()
                        .create(spreadsheetId, permission)
                        .setSendNotificationEmail(true)
                        .setSupportsAllDrives(true)
                        .execute();
            } catch (IOException exception) {
                var message = exception.getMessage() == null ? "" : exception.getMessage();
                if (!message.contains("already exists")) {
                    throw new GanttChartIntegrationException("Failed to share spreadsheet with " + email, exception);
                }
            }
        }
    }

    @Override
    public void syncContent(String spreadsheetId, GanttChartSnapshot snapshot) {
        try {
            var sheetName = resolvePrimarySheetName(spreadsheetId);
            var values = buildSheetValues(snapshot);
            var totalRows = values.size();
            var totalColumns = values.stream().mapToInt(List::size).max().orElse(FIXED_COLUMNS);
            var endColumn = columnLetter(totalColumns);

            sheetsService.spreadsheets().values()
                    .clear(spreadsheetId, quoteSheetRange(sheetName, "A1:" + endColumn + totalRows), new ValueRange())
                    .execute();

            var body = new ValueRange().setValues(toObjectValues(values));
            sheetsService.spreadsheets().values()
                    .update(spreadsheetId, quoteSheetRange(sheetName, "A1"), body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();

            var requests = buildFormatRequests(snapshot, values.size(), totalColumns);
            if (!requests.isEmpty()) {
                sheetsService.spreadsheets()
                        .batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(requests))
                        .execute();
            }
        } catch (IOException exception) {
            throw new GanttChartIntegrationException("Failed to sync Gantt chart content", exception);
        }
    }

    private String resolvePrimarySheetName(String spreadsheetId) throws IOException {
        var spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute();
        if (spreadsheet.getSheets() == null || spreadsheet.getSheets().isEmpty()) {
            return "Sheet1";
        }
        var sheetProperties = spreadsheet.getSheets().getFirst().getProperties();
        return sheetProperties != null && sheetProperties.getTitle() != null
                ? sheetProperties.getTitle()
                : "Sheet1";
    }

    private String quoteSheetRange(String sheetName, String range) {
        var escapedName = sheetName.replace("'", "''");
        return "'" + escapedName + "'!" + range;
    }

    private List<List<String>> buildSheetValues(GanttChartSnapshot snapshot) {
        var rows = new ArrayList<List<String>>();
        rows.add(List.of(snapshot.categoryName() + " — Gantt Plannia"));
        rows.add(List.of());
        rows.add(padRow(List.of("Leyenda", "Miembro"), snapshot.dateColumns().size() + FIXED_COLUMNS));

        for (var legend : snapshot.legends()) {
            rows.add(padRow(List.of("", legend.name()), snapshot.dateColumns().size() + FIXED_COLUMNS));
        }

        rows.add(List.of());

        var header = new ArrayList<String>();
        header.add("TASK");
        header.add("ASSIGNED TO");
        header.add("PROGRESS");
        header.add("START");
        header.add("END");
        snapshot.dateColumns().forEach(date -> header.add(DATE_HEADER_FORMAT.format(date)));
        rows.add(header);

        for (var taskRow : snapshot.taskRows()) {
            var row = new ArrayList<String>();
            row.add(taskRow.title());
            row.add(taskRow.assigneeName());
            row.add(taskRow.progressLabel());
            row.add(TASK_DATE_FORMAT.format(taskRow.startDate()));
            row.add(TASK_DATE_FORMAT.format(taskRow.endDate()));
            snapshot.dateColumns().forEach(date -> row.add(isDateInRange(date, taskRow) ? "■" : ""));
            rows.add(row);
        }

        return rows;
    }

    private List<Request> buildFormatRequests(GanttChartSnapshot snapshot, int totalRows, int totalColumns) {
        var requests = new ArrayList<Request>();
        var colorByUserId = new HashMap<Long, Integer>();
        snapshot.legends().forEach(legend -> colorByUserId.put(legend.userId(), legend.colorIndex()));

        var legendStartRow = 3;
        for (int index = 0; index < snapshot.legends().size(); index++) {
            var legend = snapshot.legends().get(index);
            requests.add(colorCellRequest(legendStartRow + index, 0, legend.colorIndex()));
        }

        var headerRowIndex = legendStartRow + snapshot.legends().size() + 1;
        requests.add(freezeRequest(headerRowIndex + 1, 1));
        requests.add(resizeColumnRequest(0, 220));
        requests.add(resizeColumnRequest(1, 160));

        var taskStartRow = headerRowIndex + 1;
        for (int taskIndex = 0; taskIndex < snapshot.taskRows().size(); taskIndex++) {
            var taskRow = snapshot.taskRows().get(taskIndex);
            var colorIndex = GanttChartDataBuilder.resolveColorIndex(taskRow.assigneeUserId(), colorByUserId);
            var rowIndex = taskStartRow + taskIndex;

            for (int dateIndex = 0; dateIndex < snapshot.dateColumns().size(); dateIndex++) {
                var date = snapshot.dateColumns().get(dateIndex);
                if (isDateInRange(date, taskRow)) {
                    requests.add(colorCellRequest(rowIndex, FIXED_COLUMNS + dateIndex, colorIndex));
                }
            }
        }

        if (totalColumns > 0 && totalRows > 0) {
            requests.add(boldHeaderRequest(headerRowIndex, totalColumns));
        }

        return requests;
    }

    private Request colorCellRequest(int rowIndex, int columnIndex, int colorIndex) {
        var rgb = colorIndex < 0 ? GanttColorPalette.UNASSIGNED_COLOR : GanttColorPalette.COLORS[colorIndex];
        var color = new Color()
                .setRed((float) rgb[0])
                .setGreen((float) rgb[1])
                .setBlue((float) rgb[2]);

        var cellData = new CellData().setUserEnteredFormat(
                new com.google.api.services.sheets.v4.model.CellFormat().setBackgroundColor(color)
        );

        var updateCells = new UpdateCellsRequest()
                .setRange(new GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(rowIndex)
                        .setEndRowIndex(rowIndex + 1)
                        .setStartColumnIndex(columnIndex)
                        .setEndColumnIndex(columnIndex + 1))
                .setRows(List.of(new RowData().setValues(List.of(cellData))))
                .setFields("userEnteredFormat.backgroundColor");

        return new Request().setUpdateCells(updateCells);
    }

    private Request boldHeaderRequest(int headerRowIndex, int totalColumns) {
        var boldFormat = new com.google.api.services.sheets.v4.model.CellFormat()
                .setTextFormat(new com.google.api.services.sheets.v4.model.TextFormat().setBold(true));

        var repeatCell = new RepeatCellRequest()
                .setRange(new GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(headerRowIndex)
                        .setEndRowIndex(headerRowIndex + 1)
                        .setStartColumnIndex(0)
                        .setEndColumnIndex(totalColumns))
                .setCell(new CellData().setUserEnteredFormat(boldFormat))
                .setFields("userEnteredFormat.textFormat.bold");

        return new Request().setRepeatCell(repeatCell);
    }

    private Request freezeRequest(int frozenRows, int frozenColumns) {
        var gridProperties = new com.google.api.services.sheets.v4.model.GridProperties()
                .setFrozenRowCount(frozenRows)
                .setFrozenColumnCount(frozenColumns);

        return new Request().setUpdateSheetProperties(
                new com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest()
                        .setProperties(new com.google.api.services.sheets.v4.model.SheetProperties()
                                .setSheetId(0)
                                .setGridProperties(gridProperties))
                        .setFields("gridProperties.frozenRowCount,gridProperties.frozenColumnCount")
        );
    }

    private Request resizeColumnRequest(int columnIndex, int pixelSize) {
        return new Request().setUpdateDimensionProperties(
                new UpdateDimensionPropertiesRequest()
                        .setRange(new com.google.api.services.sheets.v4.model.DimensionRange()
                                .setSheetId(0)
                                .setDimension("COLUMNS")
                                .setStartIndex(columnIndex)
                                .setEndIndex(columnIndex + 1))
                        .setProperties(new DimensionProperties().setPixelSize(pixelSize))
                        .setFields("pixelSize")
        );
    }

    private boolean isDateInRange(java.time.LocalDate date, GanttTaskRow taskRow) {
        return !date.isBefore(taskRow.startDate()) && !date.isAfter(taskRow.endDate());
    }

    private List<String> padRow(List<String> row, int targetSize) {
        var padded = new ArrayList<>(row);
        while (padded.size() < targetSize) {
            padded.add("");
        }
        return padded;
    }

    private List<List<Object>> toObjectValues(List<List<String>> values) {
        return values.stream()
                .map(row -> row.stream().<Object>map(value -> value).toList())
                .toList();
    }

    private String columnLetter(int columnCount) {
        var column = columnCount;
        var builder = new StringBuilder();
        while (column > 0) {
            var remainder = (column - 1) % 26;
            builder.insert(0, (char) ('A' + remainder));
            column = (column - 1) / 26;
        }
        return builder.toString();
    }
}
