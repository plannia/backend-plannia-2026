package upc.com.pe.backendplannia.project.application.internal.commandservices;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.project.application.internal.gantt.GanttChartDataBuilder;
import upc.com.pe.backendplannia.project.application.internal.gantt.GanttChartIntegrationException;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Category;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateCategoryGanttCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.SyncCategoryGanttCommand;
import upc.com.pe.backendplannia.project.domain.services.GanttChartCommandService;
import upc.com.pe.backendplannia.project.domain.services.GanttChartPort;
import upc.com.pe.backendplannia.project.domain.services.TeamMemberPort;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories.CategoryRepository;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories.TaskRepository;

import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "gantt.enabled", havingValue = "true")
public class GanttChartCommandServiceImpl implements GanttChartCommandService {
    private final CategoryRepository categoryRepository;
    private final TaskRepository taskRepository;
    private final GanttChartPort ganttChartPort;
    private final GanttChartDataBuilder ganttChartDataBuilder;
    private final TeamMemberPort teamMemberPort;

    public GanttChartCommandServiceImpl(
            CategoryRepository categoryRepository,
            TaskRepository taskRepository,
            GanttChartPort ganttChartPort,
            GanttChartDataBuilder ganttChartDataBuilder,
            TeamMemberPort teamMemberPort
    ) {
        this.categoryRepository = categoryRepository;
        this.taskRepository = taskRepository;
        this.ganttChartPort = ganttChartPort;
        this.ganttChartDataBuilder = ganttChartDataBuilder;
        this.teamMemberPort = teamMemberPort;
    }

    @Override
    @Transactional
    public Optional<Category> handle(CreateCategoryGanttCommand command) {
        var category = categoryRepository.findById(command.categoryId()).orElse(null);
        if (category == null) {
            return Optional.empty();
        }

        if (category.getMembers() == null || category.getMembers().isEmpty()) {
            throw new IllegalArgumentException("Category must have at least one member to create a Gantt chart");
        }

        if (category.hasGanttChart()) {
            return Optional.of(category);
        }

        try {
            var title = "Plannia - " + category.getName();
            var spreadsheet = ganttChartPort.createSpreadsheet(title);
            category.attachGanttChart(spreadsheet.spreadsheetId(), spreadsheet.spreadsheetUrl());

            var savedCategory = categoryRepository.save(category);
            syncCategoryGantt(savedCategory);
            return Optional.of(categoryRepository.findById(savedCategory.getId()).orElse(savedCategory));
        } catch (GanttChartIntegrationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new GanttChartIntegrationException("Failed to create Gantt chart in Google Sheets", exception);
        }
    }

    @Override
    @Transactional
    public void handle(SyncCategoryGanttCommand command) {
        var category = categoryRepository.findById(command.categoryId()).orElse(null);
        if (category == null || !category.hasGanttChart()) {
            return;
        }

        syncCategoryGantt(category);
    }

    private void syncCategoryGantt(Category category) {
        var tasks = taskRepository.findByCategory_Id(category.getId());
        var snapshot = ganttChartDataBuilder.build(category, tasks);
        var memberEmails = resolveMemberEmails(category);

        try {
            ganttChartPort.shareWithEmails(category.getGanttSpreadsheetId(), memberEmails);
            ganttChartPort.syncContent(category.getGanttSpreadsheetId(), snapshot);
        } catch (GanttChartIntegrationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new GanttChartIntegrationException("Failed to sync Gantt chart in Google Sheets", exception);
        }
    }

    private List<String> resolveMemberEmails(Category category) {
        return category.getMembers().stream()
                .map(member -> teamMemberPort.findEmailByUserId(member.id()).orElse(null))
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .toList();
    }
}
