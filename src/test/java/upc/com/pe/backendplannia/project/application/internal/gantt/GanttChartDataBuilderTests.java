package upc.com.pe.backendplannia.project.application.internal.gantt;

import org.junit.jupiter.api.Test;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Category;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Task;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateCategoryCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateTaskCommand;
import upc.com.pe.backendplannia.project.domain.model.readmodels.TeamMemberSnapshot;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.UserId;
import upc.com.pe.backendplannia.project.domain.services.AssignmentActivityPort;
import upc.com.pe.backendplannia.project.domain.services.TeamMemberPort;

import upc.com.pe.backendplannia.shared.test.AuditableEntityTestSupport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GanttChartDataBuilderTests {
    private static final Long MEMBER_ONE_ID = 11L;
    private static final Long MEMBER_TWO_ID = 12L;

    private final AssignmentActivityPort assignmentActivityPort = mock(AssignmentActivityPort.class);
    private final TeamMemberPort teamMemberPort = mock(TeamMemberPort.class);
    private final GanttChartDataBuilder builder = new GanttChartDataBuilder(assignmentActivityPort, teamMemberPort);

    @Test
    void includesTodoTasksGroupedByMemberInLegendOrder() {
        var category = categoryWithMembers();
        // Alice (member 1) -> in-progress ; Bob (member 2) -> done ; unassigned -> to-do
        var inProgressTask = task(category, "In progress task", Status.IN_PROGRESS,
                LocalDateTime.of(2026, 6, 20, 10, 0), null, LocalDateTime.of(2026, 6, 30, 18, 0));
        var doneTask = task(category, "Done task", Status.DONE,
                LocalDateTime.of(2026, 6, 18, 9, 0), LocalDateTime.of(2026, 6, 22, 17, 0), LocalDateTime.of(2026, 6, 25, 18, 0));
        var todoTask = task(category, "Todo task", Status.TO_DO, null, null, LocalDateTime.of(2026, 7, 1, 18, 0));

        when(assignmentActivityPort.findLatestAssignmentUserId(inProgressTask.getId())).thenReturn(Optional.of(MEMBER_ONE_ID));
        when(assignmentActivityPort.findLatestAssignmentUserId(doneTask.getId())).thenReturn(Optional.of(MEMBER_TWO_ID));
        when(teamMemberPort.findByUserId(MEMBER_ONE_ID))
                .thenReturn(Optional.of(new TeamMemberSnapshot(MEMBER_ONE_ID, "Alice", "alice@test.com")));
        when(teamMemberPort.findByUserId(MEMBER_TWO_ID))
                .thenReturn(Optional.of(new TeamMemberSnapshot(MEMBER_TWO_ID, "Bob", "bob@test.com")));
        when(teamMemberPort.findNameByUserId(MEMBER_ONE_ID)).thenReturn(Optional.of("Alice"));
        when(teamMemberPort.findNameByUserId(MEMBER_TWO_ID)).thenReturn(Optional.of("Bob"));

        var snapshot = builder.build(category, List.of(inProgressTask, doneTask, todoTask));

        // TO_DO is now included (3 rows), ordered by member (legend order), unassigned last.
        assertThat(snapshot.taskRows()).hasSize(3);
        assertThat(snapshot.taskRows().get(0).title()).isEqualTo("In progress task");
        assertThat(snapshot.taskRows().get(0).assigneeName()).isEqualTo("Alice");
        assertThat(snapshot.taskRows().get(1).title()).isEqualTo("Done task");
        assertThat(snapshot.taskRows().get(2).title()).isEqualTo("Todo task");
        assertThat(snapshot.taskRows().get(2).assigneeName()).isEqualTo("Sin asignar");
        assertThat(snapshot.legends()).hasSize(2);
    }

    @Test
    void mapsStatusProgressAndNewColumns() {
        var category = categoryWithMembers();
        var doneTask = task(category, "Done task", Status.DONE,
                LocalDateTime.of(2026, 6, 18, 9, 0), LocalDateTime.of(2026, 6, 22, 17, 0), LocalDateTime.of(2026, 6, 25, 18, 0));
        var todoTask = task(category, "Todo task", Status.TO_DO, null, null, LocalDateTime.of(2026, 7, 1, 18, 0));

        when(assignmentActivityPort.findLatestAssignmentUserId(doneTask.getId())).thenReturn(Optional.of(MEMBER_ONE_ID));
        when(teamMemberPort.findByUserId(MEMBER_ONE_ID))
                .thenReturn(Optional.of(new TeamMemberSnapshot(MEMBER_ONE_ID, "Alice", "alice@test.com")));
        when(teamMemberPort.findNameByUserId(MEMBER_ONE_ID)).thenReturn(Optional.of("Alice"));

        var snapshot = builder.build(category, List.of(doneTask, todoTask));

        var done = snapshot.taskRows().stream().filter(r -> r.title().equals("Done task")).findFirst().orElseThrow();
        assertThat(done.statusLabel()).isEqualTo("Hecho");
        assertThat(done.progressLabel()).isEqualTo("100%");
        assertThat(done.priorityLabel()).isEqualTo("Alta");
        assertThat(done.difficultyLabel()).isEqualTo("Media");
        assertThat(done.hours()).isEqualTo(4);
        assertThat(done.estimated()).isFalse();

        var todo = snapshot.taskRows().stream().filter(r -> r.title().equals("Todo task")).findFirst().orElseThrow();
        assertThat(todo.statusLabel()).isEqualTo("Pendiente");
        assertThat(todo.progressLabel()).isEqualTo("0%");
        assertThat(todo.estimated()).isTrue();
        // Estimated dates anchor on the limit date (hours=4 -> 1 workday span -> start == end == limit).
        assertThat(todo.endDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(todo.startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(todo.dueDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    void excludesCancelledTasks() {
        var category = categoryWithMembers();
        var cancelled = task(category, "Cancelled task", Status.CANCELLED, null, null, LocalDateTime.of(2026, 7, 1, 18, 0));

        var snapshot = builder.build(category, List.of(cancelled));

        assertThat(snapshot.taskRows()).isEmpty();
    }

    @Test
    void usesDefaultDateRangeWhenThereAreNoTasks() {
        var category = categoryWithMembers();
        var snapshot = builder.build(category, List.of());

        assertThat(snapshot.taskRows()).isEmpty();
        assertThat(snapshot.dateColumns()).hasSize(GanttChartDataBuilder.DEFAULT_RANGE_DAYS);
    }

    private Category categoryWithMembers() {
        var category = new Category(new CreateCategoryCommand(1L, "Planning", LocalDateTime.of(2026, 7, 31, 18, 0)));
        AuditableEntityTestSupport.assignId(category, 99L);
        category.addMember(new UserId(MEMBER_ONE_ID));
        category.addMember(new UserId(MEMBER_TWO_ID));
        return category;
    }

    private Task task(
            Category category,
            String title,
            Status status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime limitDate
    ) {
        var task = new Task(new CreateTaskCommand(
                category.getId(),
                title,
                "desc",
                4,
                "HIGH",
                "MEDIUM",
                limitDate,
                List.of(),
                List.of()
        ), category);
        AuditableEntityTestSupport.assignId(task, (long) title.hashCode());
        task.setStatus(status);
        task.setStartTime(startTime);
        task.setEndTime(endTime);
        return task;
    }
}
