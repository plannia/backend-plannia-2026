package upc.com.pe.backendplannia.project.interfaces.rest.transform;

import org.junit.jupiter.api.Test;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Category;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateCategoryCommand;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryResourceFromEntityAssemblerTests {
    @Test
    void mapsGanttSpreadsheetUrl() {
        var category = new Category(new CreateCategoryCommand(1L, "Planning", LocalDateTime.now()));
        category.setId(10L);
        category.attachGanttChart("sheet-id", "https://docs.google.com/spreadsheets/d/sheet-id/edit");

        var resource = CategoryResourceFromEntityAssembler.toResourceFromEntity(category);

        assertThat(resource.ganttSpreadsheetUrl()).isEqualTo("https://docs.google.com/spreadsheets/d/sheet-id/edit");
    }
}
