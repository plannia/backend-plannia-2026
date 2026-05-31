package upc.com.pe.backendplannia.iam.application.internal.commandservices;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.iam.application.internal.outboundedservices.HashingService;
import upc.com.pe.backendplannia.iam.application.internal.outboundedservices.TokenService;
import upc.com.pe.backendplannia.iam.domain.model.aggregates.User;
import upc.com.pe.backendplannia.iam.domain.model.commands.DeleteTeamMember;
import upc.com.pe.backendplannia.iam.domain.model.commands.SignInCommand;
import upc.com.pe.backendplannia.iam.domain.model.commands.SignUpCommand;
import upc.com.pe.backendplannia.iam.domain.model.commands.UpdateUserCommand;
import upc.com.pe.backendplannia.iam.domain.services.UserCommandService;
import upc.com.pe.backendplannia.iam.infrastructure.persistence.jpa.repositories.TeamRepository;
import upc.com.pe.backendplannia.iam.infrastructure.persistence.jpa.repositories.UserRepository;

import java.util.Optional;

@Service
public class UserCommandServiceImpl implements UserCommandService {
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;

    public UserCommandServiceImpl(
            TeamRepository teamRepository,
            UserRepository userRepository,
            HashingService hashingService,
            TokenService tokenService
    ) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
    }

    @Override
    @Transactional
    public Optional<User> handle(SignUpCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        var team = teamRepository.findByCode(command.code())
                .orElseThrow(() -> new IllegalArgumentException("Team with this code not found"));

        var user = new User(command, team, hashingService.encode(command.password()));
        team.addUser(user);

        return Optional.of(userRepository.save(user));
    }

    @Override
    public Optional<ImmutablePair<User, String>> handle(SignInCommand command) {
        var user = userRepository.findByEmail(command.email()).orElse(null);
        if (user == null || !hashingService.matches(command.password(), user.getPassword())) {
            return Optional.empty();
        }
        var token = tokenService.generateToken(user.getEmail());
        return Optional.of(ImmutablePair.of(user, token));
    }

    @Override
    public Optional<User> handle(DeleteTeamMember command) {
        throw new UnsupportedOperationException("Delete team member is not implemented yet");
    }

    @Override
    public Optional<User> handle(UpdateUserCommand command) {
        throw new UnsupportedOperationException("Update user is not implemented yet");
    }
}
