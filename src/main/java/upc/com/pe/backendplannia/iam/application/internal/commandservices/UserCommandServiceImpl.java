package upc.com.pe.backendplannia.iam.application.internal.commandservices;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.iam.application.internal.outboundedservices.HashingService;
import upc.com.pe.backendplannia.iam.application.internal.outboundedservices.TokenService;
import upc.com.pe.backendplannia.iam.domain.model.aggregates.User;
import upc.com.pe.backendplannia.iam.domain.model.commands.DeleteUserCommand;
import upc.com.pe.backendplannia.iam.domain.model.commands.SignInCommand;
import upc.com.pe.backendplannia.iam.domain.model.commands.SignUpCommand;
import upc.com.pe.backendplannia.iam.domain.model.commands.UpdateUserCommand;
import upc.com.pe.backendplannia.iam.domain.services.UserCommandService;
import upc.com.pe.backendplannia.iam.infrastructure.persistence.jpa.repositories.TeamRepository;
import upc.com.pe.backendplannia.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import upc.com.pe.backendplannia.shared.domain.model.events.UserDeletedEvent;

import java.util.Optional;

@Service
public class UserCommandServiceImpl implements UserCommandService {
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public UserCommandServiceImpl(
            TeamRepository teamRepository,
            UserRepository userRepository,
            HashingService hashingService,
            TokenService tokenService,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
        this.applicationEventPublisher = applicationEventPublisher;
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
    @Transactional
    public Optional<User> handle(DeleteUserCommand command) {
        var user = userRepository.findById(command.userId()).orElse(null);
        if (user == null) {
            return Optional.empty();
        }

        var deletedUserId = user.getId();
        user.getTeam().removeUser(user);
        userRepository.delete(user);
        applicationEventPublisher.publishEvent(new UserDeletedEvent(deletedUserId));
        return Optional.of(user);
    }

    @Override
    @Transactional
    public Optional<User> handle(UpdateUserCommand command) {
        var user = userRepository.findById(command.id()).orElse(null);
        if (user == null) {
            return Optional.empty();
        }

        if (command.email() != null && !command.email().isBlank()
                && userRepository.existsByEmailAndIdNot(command.email(), command.id())) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        var encodedPassword = command.password() != null && !command.password().isBlank()
                ? hashingService.encode(command.password())
                : null;

        user.updateUser(command, encodedPassword);
        return Optional.of(userRepository.save(user));
    }
}
