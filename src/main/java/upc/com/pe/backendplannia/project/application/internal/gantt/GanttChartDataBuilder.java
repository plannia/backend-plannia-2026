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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "gantt.enabled", havingValue = "true")
public class GanttChartDataBuilder {
    public static final int DATE_PADDING_DAYS = 3;
    public static final int DEFAULT_RANGE_DAYS = 14;
    public static final int MAX_COLOR_INDEX = 9;
    private static final int HOURS_PER_WORKDAY = 8;
    private static final int DEFAULT_ESTIMATED_SPAN_DAYS = 3;
    private static final int UNASSIGNED_ORDER = Integer.MAX_VALUE;

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
        var memberOrder = new HashMap<Long, Integer>();
        for (int index = 0; index < legends.size(); index++) {
            memberOrder.put(legends.get(index).userId(), index);
        }
        var taskRows = buildTaskRows(tasks, memberOrder);
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

    // Incluye toda tarea salvo CANCELLED. Las TO_DO (o sin startTime) entran con fechas
    // estimadas. Se agrupan por responsable (orden de la leyenda) y, dentro, por fecha.
    private List<GanttTaskRow> buildTaskRows(List<Task> tasks, Map<Long, Integer> memberOrder) {
        return tasks.stream()
                .filter(this::isIncluded)
                .map(this::toTaskRow)
                .sorted(Comparator
                        .comparingInt((GanttTaskRow row) -> memberOrder.getOrDefault(row.assigneeUserId(), UNASSIGNED_ORDER))
                        .thenComparing(GanttTaskRow::startDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private boolean isIncluded(Task task) {
        return task.getStatus() != Status.CANCELLED;
    }

    private GanttTaskRow toTaskRow(Task task) {
        var assigneeUserId = assignmentActivityPort.findLatestAssignmentUserId(task.getId()).orElse(null);
        var assigneeName = assigneeUserId == null
                ? "Sin asignar"
                : teamMemberPort.findNameByUserId(assigneeUserId).orElse("Sin asignar");

        var started = task.getStartTime() != null
                && (task.getStatus() == Status.IN_PROGRESS || task.getStatus() == Status.DONE);

        LocalDate startDate;
        LocalDate endDate;
        boolean estimated;
        if (started) {
            startDate = task.getStartTime().toLocalDate();
            endDate = resolveEndDate(task);
            estimated = false;
        } else {
            endDate = task.getLimitDate() != null
                    ? task.getLimitDate().toLocalDate()
                    : LocalDate.now().plusDays(DEFAULT_ESTIMATED_SPAN_DAYS);
            startDate = endDate.minusDays(estimatedSpanDays(task) - 1L);
            estimated = true;
        }

        return new GanttTaskRow(
                task.getId(),
                task.getTitle(),
                assigneeUserId,
                assigneeName,
                statusLabel(task.getStatus()),
                priorityLabel(task),
                difficultyLabel(task),
                task.getHours(),
                formatProgress(task),
                startDate,
                endDate,
                task.getLimitDate() != null ? task.getLimitDate().toLocalDate() : null,
                estimated
        );
    }

    private int estimatedSpanDays(Task task) {
        if (task.getHours() == null || task.getHours() <= 0) {
            return DEFAULT_ESTIMATED_SPAN_DAYS;
        }
        return Math.max(1, (int) Math.ceil((double) task.getHours() / HOURS_PER_WORKDAY));
    }

    private LocalDate resolveEndDate(Task task) {
        if (task.getEndTime() != null) {
            return task.getEndTime().toLocalDate();
        }
        return LocalDate.now();
    }

    private String statusLabel(Status status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case TO_DO -> "Pendiente";
            case IN_PROGRESS -> "En progreso";
            case DONE -> "Hecho";
            case CANCELLED -> "Cancelada";
        };
    }

    private String priorityLabel(Task task) {
        if (task.getPriority() == null) {
            return "—";
        }
        return switch (task.getPriority()) {
            case LOW -> "Baja";
            case MEDIUM -> "Media";
            case HIGH -> "Alta";
        };
    }

    private String difficultyLabel(Task task) {
        if (task.getDifficulty() == null) {
            return "—";
        }
        return switch (task.getDifficulty()) {
            case EASY -> "Fácil";
            case MEDIUM -> "Media";
            case HARD -> "Difícil";
        };
    }

    private String formatProgress(Task task) {
        if (task.getStatus() == Status.DONE) {
            return "100%";
        }
        if (task.getStatus() == Status.TO_DO) {
            return "0%";
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
