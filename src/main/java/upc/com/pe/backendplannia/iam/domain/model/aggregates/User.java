package upc.com.pe.backendplannia.iam.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import upc.com.pe.backendplannia.iam.domain.model.commands.CreateTeamCommand;
import upc.com.pe.backendplannia.iam.domain.model.commands.SignUpCommand;
import upc.com.pe.backendplannia.iam.domain.model.commands.UpdateUserCommand;
import upc.com.pe.backendplannia.iam.domain.model.valueobjects.Role;
import upc.com.pe.backendplannia.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;

@Getter
@Setter
@Entity
public class User extends AuditableAbstractAggregateRoot<User> {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String position;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    public User() {}

    public User(SignUpCommand command, Team team, String password) {
        this.name = command.name();
        this.email = command.email();
        this.password = password;
        this.position = command.position();
        this.team = team;
        this.role = Role.MEMBER;
    }

    public User(CreateTeamCommand command, Team team, String password) {
        this.name = command.leaderName();
        this.email = command.email();
        this.password = password;
        this.position = "Team Leader";
        this.team = team;
        this.role = Role.LEADER;
    }

    public void updateUser(UpdateUserCommand command, String password) {
        if (command.name() != null && !command.name().isBlank()) this.name = command.name();
        if (command.email() != null && !command.email().isBlank()) this.email = command.email();
        if (command.position() != null && !command.position().isBlank()) this.position = command.position();
        if (password != null) this.password = password;
    }
}
