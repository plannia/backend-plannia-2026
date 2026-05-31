package upc.com.pe.backendplannia.iam.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import upc.com.pe.backendplannia.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class Team extends AuditableAbstractAggregateRoot<Team> {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    protected Team() {
    }
}
