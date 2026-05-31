package upc.com.pe.backendplannia.iam.application.internal.commandservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.iam.application.internal.outboundedservices.HashingService;
import upc.com.pe.backendplannia.iam.domain.model.aggregates.Team;
import upc.com.pe.backendplannia.iam.domain.model.aggregates.User;
import upc.com.pe.backendplannia.iam.domain.model.commands.CreateTeamCommand;
import upc.com.pe.backendplannia.iam.domain.services.TeamCommandService;
import upc.com.pe.backendplannia.iam.infrastructure.persistence.jpa.repositories.TeamRepository;
import upc.com.pe.backendplannia.iam.infrastructure.persistence.jpa.repositories.UserRepository;

import java.util.Optional;

@Service
public class TeamCommandServiceImpl implements TeamCommandService {
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final HashingService hashingService;

    public TeamCommandServiceImpl(TeamRepository teamRepository, UserRepository userRepository, HashingService hashingService) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.hashingService = hashingService;
    }

    @Override
    @Transactional
    public Optional<Team> handle(CreateTeamCommand command) {
        if (teamRepository.existsByName(command.teamName())) {
            throw new IllegalArgumentException("Team with this name already exists");
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        var team = new Team(command);
        
        while (teamRepository.existsByCode(team.getCode())) {
            team.setCode(team.generateCode());
        }

        var leader = new User(command, team, hashingService.encode(command.password()));
        team.addUser(leader);

        try {
            teamRepository.save(team);
            userRepository.save(leader);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return Optional.of(team);
    }
}
