package upc.com.pe.backendplannia.project.domain.model.aggregates;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Difficulty;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Priority;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;
import upc.com.pe.backendplannia.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
public class Task extends AuditableAbstractAggregateRoot<Task> {

    private String title;
    private String description;

    private List<String> tools;
    private List<String> knowledge;

    private Integer hours;

    private Priority priority;
    private Difficulty difficulty;
    private Status status;

    private LocalDateTime limitDate;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    protected Task() {
    }
}
