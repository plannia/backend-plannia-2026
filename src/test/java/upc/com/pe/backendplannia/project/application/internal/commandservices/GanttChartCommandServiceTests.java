package upc.com.pe.backendplannia.project.application.internal.commandservices;

import org.junit.jupiter.api.Test;
import upc.com.pe.backendplannia.project.application.internal.gantt.GanttChartDataBuilder;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Category;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateCategoryCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateCategoryGanttCommand;
import upc.com.pe.backendplannia.project.domain.model.readmodels.GanttChartSnapshot;
import upc.com.pe.backendplannia.project.domain.model.readmodels.GanttSpreadsheetResult;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.UserId;
import upc.com.pe.backendplannia.project.domain.services.GanttChartPort;
import upc.com.pe.backendplannia.project.domain.services.TeamMemberPort;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories.CategoryRepository;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories.TaskRepository;

import upc.com.pe.backendplannia.shared.test.AuditableEntityTestSupport;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GanttChartCommandServiceTests {
    private static final Long CATEGORY_ID = 50L;

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final GanttChartPort ganttChartPort = mock(GanttChartPort.class);
    private final GanttChartDataBuilder ganttChartDataBuilder = mock(GanttChartDataBuilder.class);
    private final TeamMemberPort teamMemberPort = mock(TeamMemberPort.class);
    private final GanttChartCommandServiceImpl service = new GanttChartCommandServiceImpl(
            categoryRepository,
            taskRepository,
            ganttChartPort,
            ganttChartDataBuilder,
            teamMemberPort
    );

    @Test
    void createsGanttChartAndPersistsSpreadsheetLink() {
        var category = categoryWithMember();
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.findByCategory_Id(CATEGORY_ID)).thenReturn(List.of());
        when(ganttChartPort.createSpreadsheet("Plannia - Planning"))
                .thenReturn(new GanttSpreadsheetResult("sheet-123", "https://docs.google.com/spreadsheets/d/sheet-123/edit"));
        when(ganttChartDataBuilder.build(eq(category), any())).thenReturn(new GanttChartSnapshot("Planning", List.of(), List.of(), List.of()));
        when(teamMemberPort.findEmailByUserId(7L)).thenReturn(Optional.of("member@test.com"));

        var result = service.handle(new CreateCategoryGanttCommand(CATEGORY_ID));

        assertThat(result).isPresent();
        assertThat(result.get().getGanttSpreadsheetUrl()).contains("sheet-123");
        verify(ganttChartPort).shareWithEmails("sheet-123", List.of("member@test.com"));
        verify(ganttChartPort).syncContent(eq("sheet-123"), any(GanttChartSnapshot.class));
    }

    @Test
    void returnsExistingCategoryWhenGanttAlreadyExists() {
        var category = categoryWithMember();
        category.attachGanttChart("existing-sheet", "https://docs.google.com/spreadsheets/d/existing-sheet/edit");
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

        var result = service.handle(new CreateCategoryGanttCommand(CATEGORY_ID));

        assertThat(result).isPresent();
        verify(ganttChartPort, never()).createSpreadsheet(any());
    }

    @Test
    void failsWhenCategoryHasNoMembers() {
        var category = new Category(new CreateCategoryCommand(1L, "Planning", LocalDateTime.now()));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.handle(new CreateCategoryGanttCommand(CATEGORY_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one member");
    }

    private Category categoryWithMember() {
        var category = new Category(new CreateCategoryCommand(1L, "Planning", LocalDateTime.now()));
        AuditableEntityTestSupport.assignId(category, CATEGORY_ID);
        category.addMember(new UserId(7L));
        return category;
    }
}
