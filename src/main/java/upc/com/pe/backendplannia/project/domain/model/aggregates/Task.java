package upc.com.pe.backendplannia.project.domain.model.aggregates;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateTaskCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.UpdateTaskCommand;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Difficulty;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Priority;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;
import upc.com.pe.backendplannia.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class Task extends AuditableAbstractAggregateRoot<Task> {

    private String title;
    private String description;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "task_tools", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "tool")
    private List<String> tools = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "task_knowledge", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "knowledge_item")
    private List<String> knowledge = new ArrayList<>();

    private Integer hours;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime limitDate;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private boolean isAssigned;

    public Task() {}

    public Task(CreateTaskCommand command, Category category) {
        this.category = category;
        this.title = command.title();
        this.description = command.description();
        this.hours = command.hours();
        this.priority = Priority.valueOf(command.priority().toUpperCase());
        this.difficulty = Difficulty.valueOf(command.difficulty().toUpperCase());
        this.limitDate = command.limitDate();
        this.status = Status.TO_DO;

        if (command.tools() != null) {
            this.tools.addAll(command.tools());
        }
        if (command.knowledge() != null) {
            this.knowledge.addAll(command.knowledge());
        }
        this.isAssigned = false;
    }

    public void markUnassigned() {
        this.isAssigned = false;
    }

    public void markAssigned() {
        this.isAssigned = true;
    }

    public void update(UpdateTaskCommand command) {
        this.status = Status.valueOf(command.status().toUpperCase());
        if (status == Status.IN_PROGRESS) {
            if (this.startTime == null) {
                this.startTime = LocalDateTime.now();
            }
        } else if (status == Status.DONE) {
            this.endTime = LocalDateTime.now();
            if (this.startTime == null) {
                int durationHours = this.hours != null && this.hours > 0 ? this.hours : 1;
                this.startTime = this.endTime.minusHours(durationHours);
            }
        }
        if (command.limitDate() != null) {
            this.limitDate = command.limitDate();
        }
    }
}
