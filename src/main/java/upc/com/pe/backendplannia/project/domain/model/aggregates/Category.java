package upc.com.pe.backendplannia.project.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateCategoryCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.UpdateCategoryCommand;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.Status;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.TeamId;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.UserId;
import upc.com.pe.backendplannia.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class Category extends AuditableAbstractAggregateRoot<Category> {

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "team_id", nullable = false))
    })
    private TeamId teamId;

    private String name;
    private LocalDateTime limitDate;

    @Enumerated(EnumType.STRING)
    private Status status;

    // Referencias a usuarios del contexto IAM por ID (sin acoplar la entidad User).
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "category_members",
            joinColumns = @JoinColumn(name = "category_id")
    )
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "user_id", nullable = false))
    })
    private List<UserId> members = new ArrayList<>();

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Task> tasks = new ArrayList<>();

    public Category() {}

    public Category(CreateCategoryCommand command) {
        this.teamId = new TeamId(command.teamId());
        this.name = command.name();
        this.limitDate = command.limitDate();
        this.status = Status.TO_DO;
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void addMember(UserId userId) {
        if (hasMember(userId)) {
            throw new IllegalArgumentException("User is already a member of this category");
        }
        members.add(userId);
    }

    public void removeMember(UserId userId) {
        members.removeIf(member -> member.id().equals(userId.id()));
    }

    public boolean hasMember(UserId userId) {
        return members.stream().anyMatch(member -> member.id().equals(userId.id()));
    }

    public void syncStatusFromTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return;
        }
        if (tasks.stream().anyMatch(task -> task.getStatus() == Status.IN_PROGRESS)) {
            this.status = Status.IN_PROGRESS;
            return;
        }
        if (tasks.stream().allMatch(task -> task.getStatus() == Status.DONE)) {
            this.status = Status.DONE;
        }
    }

    public void update(UpdateCategoryCommand command) {
        if(command.name() != null) this.name = command.name();
        if (command.limitDate() != null) {
            this.limitDate = command.limitDate();
        }
        if (command.status() != null
                && Status.CANCELLED.name().equalsIgnoreCase(command.status().trim())) {
            this.status = Status.CANCELLED;
        }
    }
}
