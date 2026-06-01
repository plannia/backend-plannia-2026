package upc.com.pe.backendplannia.project.interfaces.rest.transform;

import upc.com.pe.backendplannia.project.domain.model.aggregates.Task;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.TaskResource;

import java.util.List;

public class TaskResourceFromEntityAssembler {
    public static TaskResource toResourceFromEntity(Task task) {
        List<String> tools = task.getTools() == null || task.getTools().isEmpty() ? null : task.getTools();
        List<String> knowledge = task.getKnowledge() == null || task.getKnowledge().isEmpty() ? null : task.getKnowledge();

        return new TaskResource(
                task.getId(),
                task.getCategory().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getHours(),
                task.getPriority(),
                task.getDifficulty(),
                task.getStatus(),
                task.getLimitDate(),
                tools,
                knowledge,
                task.getStartTime(),
                task.getEndTime()
        );
    }
}
