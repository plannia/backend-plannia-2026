package upc.com.pe.backendplannia.project.domain.model.aggregates;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.UserId;
import upc.com.pe.backendplannia.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class Category extends AuditableAbstractAggregateRoot<Category> {

    private String name;
    private LocalDateTime limitDate;

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

    protected Category() {
    }
}
