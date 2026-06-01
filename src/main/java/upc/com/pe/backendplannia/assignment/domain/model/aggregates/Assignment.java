package upc.com.pe.backendplannia.assignment.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import upc.com.pe.backendplannia.assignment.domain.model.commands.CreateAssignmentCommand;
import upc.com.pe.backendplannia.assignment.domain.model.valueobjects.AssignmentStatus;
import upc.com.pe.backendplannia.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;

@Entity
@Getter
public class Assignment extends AuditableAbstractAggregateRoot<Assignment> {
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private float skillMatch;

    @Column(nullable = false)
    private float experienceMatch;

    @Column(nullable = false)
    private float interestMatch;

    @Column(nullable = false)
    private float score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status;

    @Column(nullable = false)
    private boolean isActive;

    protected Assignment() {
    }

    public Assignment(CreateAssignmentCommand command) {
        this.userId = command.userId();
        this.taskId = command.taskId();
        this.skillMatch = command.skillMatch();
        this.experienceMatch = command.experienceMatch();
        this.interestMatch = command.interestMatch();
        this.score = command.score();
        this.status = AssignmentStatus.ACTIVE;
        this.isActive = true;
    }

    public void complete() {
        this.status = AssignmentStatus.COMPLETED;
    }

    public void cancel() {
        this.status = AssignmentStatus.CANCELLED;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
