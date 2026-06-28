package upc.com.pe.backendplannia.project.domain.services;

import upc.com.pe.backendplannia.project.domain.model.aggregates.Category;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateCategoryGanttCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.SyncCategoryGanttCommand;

import java.util.Optional;

public interface GanttChartCommandService {
    Optional<Category> handle(CreateCategoryGanttCommand command);

    void handle(SyncCategoryGanttCommand command);
}
