package upc.com.pe.backendplannia.project.domain.services;

import upc.com.pe.backendplannia.project.domain.model.aggregates.Task;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateTaskCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.MarkTaskAsAssignedCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.SyncTaskAssignmentStatusCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.UpdateTaskCommand;

import java.util.Optional;

public interface TaskCommandService {
    Optional<Task> handle(CreateTaskCommand command);

    Optional<Task> handle(MarkTaskAsAssignedCommand command);

    void handle(SyncTaskAssignmentStatusCommand command);

    Optional<Task> handle(UpdateTaskCommand command);
}