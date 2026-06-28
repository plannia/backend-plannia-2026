package upc.com.pe.backendplannia.project.domain.model.readmodels;

import java.time.LocalDate;
import java.util.List;

public record GanttChartSnapshot(
        String categoryName,
        List<GanttMemberLegend> legends,
        List<GanttTaskRow> taskRows,
        List<LocalDate> dateColumns
) {
}
