package upc.com.pe.backendplannia.project.domain.model.aggregates;

import org.junit.jupiter.api.Test;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateCategoryCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateTaskCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.UpdateTaskCommand;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskTests {
    @Test
    void updateRejectsChangingCompletedTaskToPreviousStatus() {
        var task = task();
        task.update(new UpdateTaskCommand(1L, "DONE", null));

        assertThatThrownBy(() -> task.update(new UpdateTaskCommand(1L, "IN_PROGRESS", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Completed task cannot change status");
        assertThat(task.getStatus()).isEqualTo(Status.DONE);
    }

    private Task task() {
        var category = new Category(new CreateCategoryCommand(1L, "Backend", LocalDateTime.now().plusDays(30)));
        return new Task(new CreateTaskCommand(
                1L,
                "Build endpoint",
                "Implement REST endpoint",
                4,
                "HIGH",
                "MEDIUM",
                LocalDateTime.now().plusDays(7),
                List.of("Java"),
                List.of("Spring")
        ), category);
    }
}
