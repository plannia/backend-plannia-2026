package upc.com.pe.backendplannia.iam.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import upc.com.pe.backendplannia.iam.domain.model.commands.CreateTeamCommand;
import upc.com.pe.backendplannia.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

    public Team() {}

    public Team(CreateTeamCommand command) {
        this.name = command.teamName();
        this.code = generateCode();
    }

    public String generateCode() {
        String normalized = name.trim().toUpperCase();
        String prefix = normalized.length() >= 3
                ? normalized.substring(0, 3)
                : (normalized + "XXX").substring(0, 3);
        int randomSuffix = ThreadLocalRandom.current().nextInt(1_000_000);
        return prefix + String.format("%06d", randomSuffix);
    }

    public void addUser(User user) {
        users.add(user);
    }
}
