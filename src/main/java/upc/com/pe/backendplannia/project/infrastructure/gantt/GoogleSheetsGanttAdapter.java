package upc.com.pe.backendplannia.project.infrastructure.gantt;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.DimensionProperties;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.MergeCellsRequest;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.TextFormat;
import com.google.api.services.sheets.v4.model.UnmergeCellsRequest;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.google.api.services.sheets.v4.model.UpdateDimensionPropertiesRequest;
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.project.application.internal.gantt.GanttChartIntegrationException;
import upc.com.pe.backendplannia.project.application.internal.gantt.GanttChartDataBuilder;
import upc.com.pe.backendplannia.project.domain.model.readmodels.GanttChartSnapshot;
import upc.com.pe.backendplannia.project.domain.model.readmodels.GanttSpreadsheetResult;
import upc.com.pe.backendplannia.project.domain.model.readmodels.GanttTaskRow;
import upc.com.pe.backendplannia.project.domain.services.GanttChartPort;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "gantt.enabled", havingValue = "true")
@ConditionalOnProperty(name = "gantt.google.enabled", havingValue = "true")
public class GoogleSheetsGanttAdapter implements GanttChartPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleSheetsGanttAdapter.class);
    private static final String APPLICATION_NAME = "Plannia";
    private static final int FIXED_COLUMNS = 10;
    private static final String[] FIXED_HEADERS = {
            "TAREA", "RESPONSABLE", "ESTADO", "PRIORIDAD", "DIFICULTAD", "HORAS", "AVANCE", "INICIO", "FIN", "LÍMITE"
    };
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.of("es"));
    private static final DateTimeFormatter TASK_DATE_FORMAT = DateTimeFormatter.ofPattern("d/MM");

    // Paleta de la hoja (RGB 0..1).
    private static final Color TITLE_BG = rgb(0.20, 0.28, 0.44);
    private static final Color TITLE_FG = rgb(1, 1, 1);
    private static final Color HEADER_BG = rgb(0.90, 0.92, 0.96);
    private static final Color SECTION_BG = rgb(0.94, 0.95, 0.97);
    private static final Color WEEKEND_BG = rgb(0.95, 0.96, 0.98);
    private static final Color TODAY_BG = rgb(1.0, 0.89, 0.60);
    private static final Color WHITE = rgb(1, 1, 1);

    private final GanttGoogleProperties properties;
    private final GoogleGanttCredentialsProvider credentialsProvider;
    private Sheets sheetsService;
    private Drive driveService;
    // null = inicializó OK; no-null = motivo por el que Gantt-Google quedó inactivo.
    private String initError;

    public GoogleSheetsGanttAdapter(GanttGoogleProperties properties, GoogleGanttCredentialsProvider credentialsProvider) {
        this.properties = properties;
        this.credentialsProvider = credentialsProvider;
    }

    @PostConstruct
    void init() {
        // Fail-soft: Gantt es opcional; una config inválida NO debe tumbar el backend.
        try {
            if (!properties.hasOutputFolder()) {
                disableGantt("gantt.google.output-folder-id (GANTT_OUTPUT_FOLDER_ID) is required. "
                        + "Use a folder in your Google Drive (OAuth/Gmail) or inside a Shared Drive (service account).");
                return;
            }

            var credentials = credentialsProvider.build();
            var requestInitializer = new HttpCredentialsAdapter(credentials);
            var transport = GoogleNetHttpTransport.newTrustedTransport();
            var jsonFactory = GsonFactory.getDefaultInstance();

            sheetsService = new Sheets.Builder(transport, jsonFactory, requestInitializer)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            driveService = new Drive.Builder(transport, jsonFactory, requestInitializer)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            var folderError = validateOutputFolder();
            if (folderError != null) {
                disableGantt(folderError);
                return;
            }

            initError = null;
            LOGGER.info("Gantt Google Sheets inicializado correctamente.");
        } catch (Exception exception) {
            disableGantt(exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName(), exception);
        }
    }

    private void disableGantt(String reason) {
        disableGantt(reason, null);
    }

    private void disableGantt(String reason, Throwable cause) {
        initError = reason;
        sheetsService = null;
        driveService = null;
        if (cause != null) {
            LOGGER.error("Gantt Google deshabilitado: {}. El backend arranca igual; solo /gantt devolverá 502.", reason, cause);
        } else {
            LOGGER.warn("Gantt Google deshabilitado: {}. El backend arranca igual; solo /gantt devolverá 502.", reason);
        }
    }

    /** true si Sheets+Drive inicializaron OK en el arranque. Lo consume /api/v1/gantt/status. */
    public boolean isReady() {
        return sheetsService != null && driveService != null;
    }

    /** Motivo por el que Gantt-Google quedó inactivo, o null si inicializó bien. */
    public String initError() {
        return initError;
    }

    // Lanza un error claro (lo traduce el controller a 502) si la integración no quedó inicializada.
    private void ensureReady() {
        if (sheetsService == null || driveService == null) {
            throw new GanttChartIntegrationException(
                    "La integración Gantt con Google no está disponible: "
                            + (initError != null ? initError : "no inicializada"));
        }
    }

    @Override
    public GanttSpreadsheetResult createSpreadsheet(String title) {
        ensureReady();
        try {
            // Create in the output folder; syncContent fills the Gantt data programmatically.
            if (properties.hasOutputFolder()) {
                return createEmptySpreadsheet(title);
            }
            if (properties.hasTemplateSheet()) {
                return copyTemplateSpreadsheet(title);
            }
            return createEmptySpreadsheet(title);
        } catch (IOException exception) {
            throw new GanttChartIntegrationException(
                    "Failed to create Google Spreadsheet: " + describeCreateFailure(exception),
                    exception
            );
        }
    }

    /** @return null if valid; otherwise a human-readable reason why Gantt was disabled */
    private String validateOutputFolder() {
        try {
            var folder = driveService.files().get(properties.getOutputFolderId())
                    .setSupportsAllDrives(true)
                    .setFields("id,name,driveId,mimeType,trashed")
                    .execute();
            if (Boolean.TRUE.equals(folder.getTrashed())) {
                return "GANTT_OUTPUT_FOLDER_ID points to a trashed folder: " + properties.getOutputFolderId();
            }
            if ((folder.getDriveId() == null || folder.getDriveId().isBlank()) && properties.usesServiceAccount()) {
                return "GANTT_OUTPUT_FOLDER_ID must be inside a Google Shared Drive when using a service account. "
                        + "Folder '" + folder.getName() + "' is in My Drive. "
                        + "For personal Gmail, set GANTT_OAUTH_REFRESH_TOKEN (and remove GANTT_GOOGLE_CREDENTIALS_JSON).";
            }
            return null;
        } catch (IOException exception) {
            var hint = properties.usesOAuthUser()
                    ? "Ensure you authorized the same Gmail account that owns the folder."
                    : "Ensure the service account is Content manager on the Shared Drive.";
            return "Cannot access Gantt output folder " + properties.getOutputFolderId() + ": "
                    + GoogleApiIOExceptionHelper.describe(exception) + ". " + hint;
        }
    }

    private GanttSpreadsheetResult copyTemplateSpreadsheet(String title) throws IOException {
        var metadata = new File().setName(title);
        applyOutputFolder(metadata);
        var copied = driveService.files()
                .copy(properties.getTemplateSheetId(), metadata)
                .setSupportsAllDrives(true)
                .execute();
        return toSpreadsheetResult(copied.getId());
    }

    private GanttSpreadsheetResult createEmptySpreadsheet(String title) throws IOException {
        if (properties.hasOutputFolder()) {
            var metadata = new File()
                    .setName(title)
                    .setMimeType("application/vnd.google-apps.spreadsheet");
            metadata.setParents(List.of(properties.getOutputFolderId()));
            var created = driveService.files()
                    .create(metadata)
                    .setSupportsAllDrives(true)
                    .execute();
            return toSpreadsheetResult(created.getId());
        }

        var spreadsheet = new Spreadsheet()
                .setProperties(new SpreadsheetProperties().setTitle(title));
        var created = sheetsService.spreadsheets().create(spreadsheet).execute();
        return toSpreadsheetResult(created.getSpreadsheetId());
    }

    private void applyOutputFolder(File metadata) {
        if (properties.hasOutputFolder()) {
            metadata.setParents(List.of(properties.getOutputFolderId()));
        }
    }

    private GanttSpreadsheetResult toSpreadsheetResult(String spreadsheetId) {
        var spreadsheetUrl = "https://docs.google.com/spreadsheets/d/" + spreadsheetId + "/edit";
        return new GanttSpreadsheetResult(spreadsheetId, spreadsheetUrl);
    }

    private String describeCreateFailure(IOException exception) {
        var detail = GoogleApiIOExceptionHelper.describe(exception);
        if (detail.contains("storageQuotaExceeded") || detail.toLowerCase(Locale.ROOT).contains("storage quota")) {
            if (properties.usesServiceAccount()) {
                return detail + ". Service accounts cannot use personal Drive storage; set GANTT_OUTPUT_FOLDER_ID to a folder "
                        + "inside a Shared Drive, or switch to OAuth user mode (GANTT_OAUTH_*) for personal Gmail.";
            }
            return detail + ". Check that the authorized Gmail account has enough Drive storage.";
        }
        return detail;
    }

    @Override
    public void shareWithEmails(String spreadsheetId, List<String> emails) {
        ensureReady();
        // Solo compartimos con quien AÚN no tiene acceso. Re-crear un permiso existente igual cuenta
        // contra el rate limit de compartir de Google (y reenvía notificación), y en cada re-sync
        // explotaba con "límite para compartir". Listamos una vez y filtramos.
        var alreadyShared = existingSharedEmails(spreadsheetId);
        for (var email : emails) {
            if (email == null || email.isBlank() || alreadyShared.contains(email.toLowerCase(Locale.ROOT))) {
                continue;
            }
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
                    throw new GanttChartIntegrationException(
                            "Failed to share spreadsheet with " + email + ": " + GoogleApiIOExceptionHelper.describe(exception),
                            exception
                    );
                }
            }
        }
    }

    /** Emails que ya tienen permiso en la hoja (en minúscula). Si falla la lectura, devuelve vacío. */
    private Set<String> existingSharedEmails(String spreadsheetId) {
        try {
            var response = driveService.permissions().list(spreadsheetId)
                    .setFields("permissions(emailAddress)")
                    .setSupportsAllDrives(true)
                    .execute();
            if (response.getPermissions() == null) {
                return Set.of();
            }
            var shared = new HashSet<String>();
            for (var permission : response.getPermissions()) {
                if (permission.getEmailAddress() != null) {
                    shared.add(permission.getEmailAddress().toLowerCase(Locale.ROOT));
                }
            }
            return shared;
        } catch (IOException exception) {
            LOGGER.warn("No se pudieron listar permisos de {}: {}. Se intentará compartir de todos modos.",
                    spreadsheetId, GoogleApiIOExceptionHelper.describe(exception));
            return Set.of();
        }
    }

    @Override
    public void syncContent(String spreadsheetId, GanttChartSnapshot snapshot) {
        ensureReady();
        try {
            var spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute();
            var sheetName = primarySheetName(spreadsheet);
            var existingMerges = primarySheetMerges(spreadsheet);

            var rows = buildRows(snapshot);
            var totalColumns = FIXED_COLUMNS + snapshot.dateColumns().size();
            var totalRows = rows.size();
            var endColumn = columnLetter(totalColumns);

            sheetsService.spreadsheets().values()
                    .clear(spreadsheetId, quoteSheetRange(sheetName, "A1:" + endColumn + (totalRows + 5)), new ClearValuesRequest())
                    .execute();

            var values = rows.stream().map(row -> (List<Object>) new ArrayList<Object>(row.values())).toList();
            var body = new ValueRange().setValues(values);
            sheetsService.spreadsheets().values()
                    .update(spreadsheetId, quoteSheetRange(sheetName, "A1"), body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();

            var requests = new ArrayList<Request>();
            // Quita los merges de la sync anterior: si no, un merge nuevo que se solape con uno
            // viejo (las fechas cambian entre syncs) hace que Google rechace todo el batch.
            for (var merge : existingMerges) {
                requests.add(new Request().setUnmergeCells(new UnmergeCellsRequest().setRange(merge)));
            }
            requests.addAll(buildFormatRequests(rows, snapshot, totalRows, totalColumns));
            if (!requests.isEmpty()) {
                sheetsService.spreadsheets()
                        .batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(requests))
                        .execute();
            }
        } catch (IOException exception) {
            throw new GanttChartIntegrationException(
                    "Failed to sync Gantt chart content: " + GoogleApiIOExceptionHelper.describe(exception),
                    exception
            );
        }
    }

    private String primarySheetName(Spreadsheet spreadsheet) {
        if (spreadsheet.getSheets() == null || spreadsheet.getSheets().isEmpty()) {
            return "Sheet1";
        }
        var sheetProperties = spreadsheet.getSheets().getFirst().getProperties();
        return sheetProperties != null && sheetProperties.getTitle() != null
                ? sheetProperties.getTitle()
                : "Sheet1";
    }

    private List<GridRange> primarySheetMerges(Spreadsheet spreadsheet) {
        if (spreadsheet.getSheets() == null || spreadsheet.getSheets().isEmpty()) {
            return List.of();
        }
        var merges = spreadsheet.getSheets().getFirst().getMerges();
        return merges != null ? merges : List.of();
    }

    private String quoteSheetRange(String sheetName, String range) {
        var escapedName = sheetName.replace("'", "''");
        return "'" + escapedName + "'!" + range;
    }

    // ---------- Construcción de filas (valores + metadatos para formatear) ----------

    private enum Kind { TITLE, SPACER, LEGEND_HEADER, LEGEND, SECTION, MONTH, HEADER, TASK }

    private record Row(Kind kind, List<String> values, GanttTaskRow task, int colorIndex) {
    }

    private List<Row> buildRows(GanttChartSnapshot snapshot) {
        var totalColumns = FIXED_COLUMNS + snapshot.dateColumns().size();
        var colorByUser = new HashMap<Long, Integer>();
        snapshot.legends().forEach(legend -> colorByUser.put(legend.userId(), legend.colorIndex()));

        var rows = new ArrayList<Row>();
        rows.add(new Row(Kind.TITLE, pad(List.of(snapshot.categoryName() + " — Gantt Plannia"), totalColumns), null, -1));
        rows.add(new Row(Kind.SPACER, pad(List.of(), totalColumns), null, -1));

        rows.add(new Row(Kind.LEGEND_HEADER, pad(List.of("MIEMBROS"), totalColumns), null, -1));
        for (var legend : snapshot.legends()) {
            rows.add(new Row(Kind.LEGEND, pad(List.of("", legend.name(), legend.email()), totalColumns), null, legend.colorIndex()));
        }
        rows.add(new Row(Kind.SPACER, pad(List.of(), totalColumns), null, -1));

        rows.add(new Row(Kind.MONTH, monthRow(snapshot.dateColumns(), totalColumns), null, -1));
        rows.add(new Row(Kind.HEADER, headerRow(snapshot.dateColumns()), null, -1));

        Long currentAssignee = null;
        var first = true;
        for (var task : snapshot.taskRows()) {
            if (first || !Objects.equals(task.assigneeUserId(), currentAssignee)) {
                currentAssignee = task.assigneeUserId();
                first = false;
                var sectionColor = GanttChartDataBuilder.resolveColorIndex(currentAssignee, colorByUser);
                rows.add(new Row(Kind.SECTION, pad(List.of("▸ " + task.assigneeName()), totalColumns), null, sectionColor));
            }
            var colorIndex = GanttChartDataBuilder.resolveColorIndex(task.assigneeUserId(), colorByUser);
            rows.add(new Row(Kind.TASK, taskRow(task, snapshot.dateColumns()), task, colorIndex));
        }
        return rows;
    }

    private List<String> monthRow(List<LocalDate> dateColumns, int totalColumns) {
        var row = new ArrayList<String>();
        for (int i = 0; i < FIXED_COLUMNS; i++) {
            row.add("");
        }
        YearMonth previous = null;
        for (var date : dateColumns) {
            var current = YearMonth.from(date);
            if (!current.equals(previous)) {
                row.add(capitalize(MONTH_FORMAT.format(date)));
                previous = current;
            } else {
                row.add("");
            }
        }
        return pad(row, totalColumns);
    }

    private List<String> headerRow(List<LocalDate> dateColumns) {
        var row = new ArrayList<String>(List.of(FIXED_HEADERS));
        dateColumns.forEach(date -> row.add(String.valueOf(date.getDayOfMonth())));
        return row;
    }

    private List<String> taskRow(GanttTaskRow task, List<LocalDate> dateColumns) {
        var row = new ArrayList<String>();
        row.add(task.title());
        row.add(task.assigneeName());
        row.add(task.statusLabel());
        row.add(task.priorityLabel());
        row.add(task.difficultyLabel());
        row.add(task.hours() == null ? "" : String.valueOf(task.hours()));
        row.add(progressBar(task.progressLabel()));
        row.add(TASK_DATE_FORMAT.format(task.startDate()));
        row.add(TASK_DATE_FORMAT.format(task.endDate()));
        row.add(task.dueDate() != null ? TASK_DATE_FORMAT.format(task.dueDate()) : "");
        var bar = task.estimated() ? "▒" : "■";
        dateColumns.forEach(date -> row.add(isDateInRange(date, task) ? bar : ""));
        return row;
    }

    private String progressBar(String progressLabel) {
        var pct = parsePercent(progressLabel);
        var filled = Math.max(0, Math.min(10, Math.round(pct / 10f)));
        return "█".repeat(filled) + "░".repeat(10 - filled) + "  " + progressLabel;
    }

    private int parsePercent(String label) {
        try {
            return Integer.parseInt(label.replace("%", "").trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    // ---------- Formato ----------

    private List<Request> buildFormatRequests(List<Row> rows, GanttChartSnapshot snapshot, int totalRows, int totalColumns) {
        var requests = new ArrayList<Request>();
        var dateColumns = snapshot.dateColumns();

        var headerRowIndex = indexOf(rows, Kind.HEADER);
        var firstTaskRegionRow = headerRowIndex + 1;
        var today = LocalDate.now();

        // 0) Reset de formato en toda el área usada (evita colores viejos en re-syncs).
        requests.add(fillRange(0, totalRows, 0, totalColumns, WHITE, false));

        // 1) Sombreado de columnas (fin de semana + hoy) sobre la zona de tareas. Va ANTES de las barras.
        for (int d = 0; d < dateColumns.size(); d++) {
            var date = dateColumns.get(d);
            var col = FIXED_COLUMNS + d;
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                requests.add(fillRange(firstTaskRegionRow, totalRows, col, col + 1, WEEKEND_BG, false));
            }
            if (date.equals(today)) {
                requests.add(fillRange(firstTaskRegionRow, totalRows, col, col + 1, TODAY_BG, false));
            }
        }

        // 2) Barras de las tareas (pintan sobre el sombreado).
        for (int r = 0; r < rows.size(); r++) {
            var row = rows.get(r);
            if (row.kind() != Kind.TASK) {
                continue;
            }
            for (int d = 0; d < dateColumns.size(); d++) {
                if (isDateInRange(dateColumns.get(d), row.task())) {
                    requests.add(barCell(r, FIXED_COLUMNS + d, row.colorIndex(), row.task().estimated()));
                }
            }
        }

        // 3) Estructura y estilos (no compiten con las barras del grid).
        for (int r = 0; r < rows.size(); r++) {
            var row = rows.get(r);
            switch (row.kind()) {
                // El título NO se combina: una celda combinada a todo lo ancho choca con el
                // freeze de la primera columna. El texto en A1 se desborda sobre el fondo coloreado.
                case TITLE -> requests.add(titleFormat(r, totalColumns));
                case LEGEND_HEADER -> requests.add(boldRange(r, 0, 1));
                case LEGEND -> requests.add(barCell(r, 0, row.colorIndex(), false));
                case SECTION -> requests.add(sectionFormat(r, totalColumns, row.colorIndex()));
                case MONTH -> requests.addAll(monthFormat(r, dateColumns));
                case HEADER -> requests.add(headerFormat(r, totalColumns));
                default -> {
                    // TASK/SPACER: sin estilo extra aquí.
                }
            }
        }

        // 4) Congelar (header + primera columna) y anchos.
        requests.add(freezeRequest(headerRowIndex + 1, 1));
        requests.add(resizeColumnRequest(0, 240));
        requests.add(resizeColumnRequest(1, 150));
        for (int d = 0; d < dateColumns.size(); d++) {
            requests.add(resizeColumnRequest(FIXED_COLUMNS + d, 26));
        }

        return requests;
    }

    private int indexOf(List<Row> rows, Kind kind) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).kind() == kind) {
                return i;
            }
        }
        return 0;
    }

    private Request barCell(int rowIndex, int columnIndex, int colorIndex, boolean dim) {
        var rgb = colorIndex < 0 ? GanttColorPalette.UNASSIGNED_COLOR : GanttColorPalette.COLORS[colorIndex];
        var color = dim ? rgb(blend(rgb[0]), blend(rgb[1]), blend(rgb[2])) : rgb(rgb[0], rgb[1], rgb[2]);
        return new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(range(rowIndex, rowIndex + 1, columnIndex, columnIndex + 1))
                .setCell(new CellData().setUserEnteredFormat(new CellFormat().setBackgroundColor(color)))
                .setFields("userEnteredFormat.backgroundColor"));
    }

    private Request fillRange(int startRow, int endRow, int startCol, int endCol, Color color, boolean bold) {
        var format = new CellFormat().setBackgroundColor(color);
        var fields = "userEnteredFormat.backgroundColor";
        if (bold) {
            format.setTextFormat(new TextFormat().setBold(true));
            fields += ",userEnteredFormat.textFormat.bold";
        } else {
            format.setTextFormat(new TextFormat().setBold(false));
            fields += ",userEnteredFormat.textFormat.bold";
        }
        return new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(range(startRow, endRow, startCol, endCol))
                .setCell(new CellData().setUserEnteredFormat(format))
                .setFields(fields));
    }

    private Request titleFormat(int rowIndex, int totalColumns) {
        var format = new CellFormat()
                .setBackgroundColor(TITLE_BG)
                .setHorizontalAlignment("LEFT")
                .setVerticalAlignment("MIDDLE")
                .setTextFormat(new TextFormat().setBold(true).setFontSize(14).setForegroundColor(TITLE_FG));
        return new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(range(rowIndex, rowIndex + 1, 0, totalColumns))
                .setCell(new CellData().setUserEnteredFormat(format))
                .setFields("userEnteredFormat(backgroundColor,horizontalAlignment,verticalAlignment,textFormat)"));
    }

    private Request headerFormat(int rowIndex, int totalColumns) {
        var format = new CellFormat()
                .setBackgroundColor(HEADER_BG)
                .setHorizontalAlignment("CENTER")
                .setTextFormat(new TextFormat().setBold(true));
        return new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(range(rowIndex, rowIndex + 1, 0, totalColumns))
                .setCell(new CellData().setUserEnteredFormat(format))
                .setFields("userEnteredFormat(backgroundColor,horizontalAlignment,textFormat)"));
    }

    private Request sectionFormat(int rowIndex, int totalColumns, int colorIndex) {
        var format = new CellFormat()
                .setBackgroundColor(SECTION_BG)
                .setTextFormat(new TextFormat().setBold(true));
        return new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(range(rowIndex, rowIndex + 1, 0, totalColumns))
                .setCell(new CellData().setUserEnteredFormat(format))
                .setFields("userEnteredFormat(backgroundColor,textFormat)"));
    }

    private List<Request> monthFormat(int rowIndex, List<LocalDate> dateColumns) {
        var requests = new ArrayList<Request>();
        int runStart = 0;
        YearMonth runMonth = dateColumns.isEmpty() ? null : YearMonth.from(dateColumns.getFirst());
        for (int i = 1; i <= dateColumns.size(); i++) {
            var current = i < dateColumns.size() ? YearMonth.from(dateColumns.get(i)) : null;
            if (current == null || !current.equals(runMonth)) {
                var startCol = FIXED_COLUMNS + runStart;
                var endCol = FIXED_COLUMNS + i;
                if (endCol - startCol > 1) {
                    requests.add(new Request().setMergeCells(new MergeCellsRequest()
                            .setRange(range(rowIndex, rowIndex + 1, startCol, endCol))
                            .setMergeType("MERGE_ALL")));
                }
                requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                        .setRange(range(rowIndex, rowIndex + 1, startCol, endCol))
                        .setCell(new CellData().setUserEnteredFormat(new CellFormat()
                                .setHorizontalAlignment("CENTER")
                                .setTextFormat(new TextFormat().setBold(true))))
                        .setFields("userEnteredFormat(horizontalAlignment,textFormat)")));
                runStart = i;
                runMonth = current;
            }
        }
        return requests;
    }

    private Request boldRange(int rowIndex, int startCol, int endCol) {
        return new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(range(rowIndex, rowIndex + 1, startCol, endCol))
                .setCell(new CellData().setUserEnteredFormat(new CellFormat().setTextFormat(new TextFormat().setBold(true))))
                .setFields("userEnteredFormat.textFormat.bold"));
    }

    private Request freezeRequest(int frozenRows, int frozenColumns) {
        var gridProperties = new GridProperties()
                .setFrozenRowCount(frozenRows)
                .setFrozenColumnCount(frozenColumns);
        return new Request().setUpdateSheetProperties(new UpdateSheetPropertiesRequest()
                .setProperties(new SheetProperties().setSheetId(0).setGridProperties(gridProperties))
                .setFields("gridProperties.frozenRowCount,gridProperties.frozenColumnCount"));
    }

    private Request resizeColumnRequest(int columnIndex, int pixelSize) {
        return new Request().setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
                .setRange(new DimensionRange().setSheetId(0).setDimension("COLUMNS")
                        .setStartIndex(columnIndex).setEndIndex(columnIndex + 1))
                .setProperties(new DimensionProperties().setPixelSize(pixelSize))
                .setFields("pixelSize"));
    }

    private GridRange range(int startRow, int endRow, int startCol, int endCol) {
        return new GridRange().setSheetId(0)
                .setStartRowIndex(startRow).setEndRowIndex(endRow)
                .setStartColumnIndex(startCol).setEndColumnIndex(endCol);
    }

    private boolean isDateInRange(LocalDate date, GanttTaskRow taskRow) {
        return !date.isBefore(taskRow.startDate()) && !date.isAfter(taskRow.endDate());
    }

    private List<String> pad(List<String> row, int targetSize) {
        var padded = new ArrayList<>(row);
        while (padded.size() < targetSize) {
            padded.add("");
        }
        return padded;
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

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static double blend(double channel) {
        return channel + (1.0 - channel) * 0.55;
    }

    private static Color rgb(double red, double green, double blue) {
        return new Color()
                .setRed((float) red)
                .setGreen((float) green)
                .setBlue((float) blue);
    }
}
