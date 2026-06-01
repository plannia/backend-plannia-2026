package upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.specifications;

import org.springframework.data.jpa.domain.Specification;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Task;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Difficulty;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Priority;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;

import java.util.Collection;

public final class TaskSpecifications {
    private TaskSpecifications() {
    }

    public static Specification<Task> byTeamId(Long teamId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("category").get("teamId").get("id"), teamId);
    }

    public static Specification<Task> titleContains(String title) {
        var pattern = "%" + title.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
    }

    public static Specification<Task> descriptionContains(String description) {
        var pattern = "%" + description.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern);
    }

    public static Specification<Task> hasPriority(Priority priority) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("priority"), priority);
    }

    public static Specification<Task> hasDifficulty(Difficulty difficulty) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Task> hasStatus(Status status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Task> hasCategoryId(Long categoryId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Task> taskIdIn(Collection<Long> taskIds) {
        return (root, query, criteriaBuilder) -> root.get("id").in(taskIds);
    }

    public static Specification<Task> isAssigned(boolean assigned) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isAssigned"), assigned);
    }
}
