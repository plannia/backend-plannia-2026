package upc.com.pe.backendplannia.project.infrastructure.gantt;

import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.SheetsScopes;

import java.util.List;

final class GanttOAuthScopes {
    static final List<String> ALL = List.of(
            SheetsScopes.SPREADSHEETS,
            DriveScopes.DRIVE,
            DriveScopes.DRIVE_FILE
    );

    private GanttOAuthScopes() {
    }
}
