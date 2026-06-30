package upc.com.pe.backendplannia.project.infrastructure.acl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.TaskRequirement;
import upc.com.pe.backendplannia.assignment.domain.services.TaskRequirementResolver;
import upc.com.pe.backendplannia.profile.application.internal.outboundservices.ai.ProfileEmbeddingService;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Task;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories.TaskRepository;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProjectContextTaskRequirementResolver implements TaskRequirementResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectContextTaskRequirementResolver.class);

    private final TaskRepository taskRepository;
    private final ProfileEmbeddingService profileEmbeddingService;

    public ProjectContextTaskRequirementResolver(
            TaskRepository taskRepository,
            ProfileEmbeddingService profileEmbeddingService
    ) {
        this.taskRepository = taskRepository;
        this.profileEmbeddingService = profileEmbeddingService;
    }

    @Override
    public Optional<TaskRequirement> resolveByTaskId(Long taskId) {
        LOGGER.info("Resolving task requirement: taskId={}", taskId);
        return taskRepository.findById(taskId).map(this::toTaskRequirement);
    }

    private TaskRequirement toTaskRequirement(Task task) {
        var description = buildTaskDescription(task);
        LOGGER.info(
                "Task requirement source loaded: taskId={}, hours={}, priority={}, difficulty={}, descriptionLength={}",
                task.getId(),
                task.getHours(),
                task.getPriority(),
                task.getDifficulty(),
                description.length()
        );

        try {
            var embedding = profileEmbeddingService.generateEmbedding(description);
            LOGGER.info(
                    "Task requirement embedding generated: taskId={}, embeddingDim={}",
                    task.getId(),
                    embedding.dimension()
            );
            return new TaskRequirement(
                    task.getId(),
                    embedding,
                    task.getHours(),
                    task.getPriority().name(),
                    task.getDifficulty().name()
            );
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Failed to generate task requirement embedding: taskId={}, descriptionLength={}",
                    task.getId(),
                    description.length(),
                    exception
            );
            throw exception;
        }
    }

    private static String buildTaskDescription(Task task) {
        return Stream.of(
                        task.getTitle(),
                        task.getDescription(),
                        joinValues("Tools", task.getTools()),
                        joinValues("Knowledge", task.getKnowledge())
                )
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(". "));
    }

    private static String joinValues(String label, java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return label + ": " + String.join(", ", values);
    }
}
