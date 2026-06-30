package upc.com.pe.backendplannia.project.application.internal.gantt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Category;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Task;
import upc.com.pe.backendplannia.project.domain.model.readmodels.GanttChartSnapshot;
import upc.com.pe.backendplannia.project.domain.model.readmodels.GanttMemberLegend;
import upc.com.pe.backendplannia.project.domain.model.readmodels.GanttTaskRow;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;
import upc.com.pe.backendplannia.project.domain.services.AssignmentActivityPort;
import upc.com.pe.backendplannia.project.domain.services.TeamMemberPort;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "gantt.enabled", havingValue = "true")
public class GanttChartDataBuilder {
    public static final int DATE_PADDING_DAYS = 3;
    public static final int DEFAULT_RANGE_DAYS = 14;
    public static final int MAX_COLOR_INDEX = 9;

    private final AssignmentActivityPort assignmentActivityPort;
    private final TeamMemberPort teamMemberPort;

    public GanttChartDataBuilder(
            AssignmentActivityPort assignmentActivityPort,
            TeamMemberPort teamMemberPort
    ) {
        this.assignmentActivityPort = assignmentActivityPort;
        this.teamMemberPort = teamMemberPort;
    }

    public GanttChartSnapshot build(Category category, List<Task> tasks) {
        var legends = buildLegends(category);
        var taskRows = buildTaskRows(tasks);
        var dateColumns = buildDateColumns(taskRows);

        return new GanttChartSnapshot(category.getName(), legends, taskRows, dateColumns);
    }

    private List<GanttMemberLegend> buildLegends(Category category) {
        var members = category.getMembers();
        if (members == null || members.isEmpty()) {
            return List.of();
        }

        var legends = new ArrayList<GanttMemberLegend>();
        for (int index = 0; index < members.size(); index++) {
            var memberId = members.get(index).id();
            var colorIndex = index % (MAX_COLOR_INDEX + 1);
            teamMemberPort.findByUserId(memberId).ifPresent(member ->
                    legends.add(new GanttMemberLegend(
                            member.userId(),
                            member.name(),
                            member.email(),
                            colorIndex
                    ))
            );
        }
        return legends;
    }

    private List<GanttTaskRow> buildTaskRows(List<Task> tasks) {
        return tasks.stream()
                .filter(this::isGanttEligible)
                .sorted(Comparator.comparing(Task::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toTaskRow)
                .toList();
    }

    private GanttTaskRow toTaskRow(Task task) {
        var assigneeUserId = assignmentActivityPort.findLatestAssignmentUserId(task.getId()).orElse(null);
        var assigneeName = assigneeUserId == null
                ? "Sin asignar"
                : teamMemberPort.findNameByUserId(assigneeUserId).orElse("Sin asignar");

        var startDate = task.getStartTime().toLocalDate();
        var endDate = resolveEndDate(task);

        return new GanttTaskRow(
                task.getId(),
                task.getTitle(),
                assigneeUserId,
                assigneeName,
                formatProgress(task),
                startDate,
                endDate
        );
    }

    private boolean isGanttEligible(Task task) {
        if (task.getStartTime() == null) {
            return false;
        }
        return task.getStatus() == Status.IN_PROGRESS || task.getStatus() == Status.DONE;
    }

    private LocalDate resolveEndDate(Task task) {
        if (task.getEndTime() != null) {
            return task.getEndTime().toLocalDate();
        }
        return LocalDate.now();
    }

    private String formatProgress(Task task) {
        if (task.getStatus() == Status.DONE) {
            return "100%";
        }

        if (task.getLimitDate() == null || task.getStartTime() == null) {
            return "50%";
        }

        var start = task.getStartTime();
        var limit = task.getLimitDate();
        if (!limit.isAfter(start)) {
            return "50%";
        }

        var totalDays = ChronoUnit.DAYS.between(start.toLocalDate(), limit.toLocalDate());
        if (totalDays <= 0) {
            return "50%";
        }

        var elapsedDays = ChronoUnit.DAYS.between(start.toLocalDate(), LocalDate.now());
        var progress = (int) Math.min(99, Math.max(1, (elapsedDays * 100) / totalDays));
        return progress + "%";
    }

    private List<LocalDate> buildDateColumns(List<GanttTaskRow> taskRows) {
        if (taskRows.isEmpty()) {
            var start = LocalDate.now();
            return start.datesUntil(start.plusDays(DEFAULT_RANGE_DAYS)).toList();
        }

        var minStart = taskRows.stream()
                .map(GanttTaskRow::startDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        var maxEnd = taskRows.stream()
                .map(GanttTaskRow::endDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        var rangeStart = minStart.minusDays(DATE_PADDING_DAYS);
        var rangeEnd = maxEnd.plusDays(DATE_PADDING_DAYS);
        if (rangeEnd.isBefore(rangeStart)) {
            rangeEnd = rangeStart.plusDays(DEFAULT_RANGE_DAYS);
        }

        return rangeStart.datesUntil(rangeEnd.plusDays(1)).toList();
    }

    public static int resolveColorIndex(Long assigneeUserId, Map<Long, Integer> colorByUserId) {
        if (assigneeUserId == null) {
            return -1;
        }
        return colorByUserId.getOrDefault(assigneeUserId, -1);
    }
}
