package upc.com.pe.backendplannia.iam.domain.services;

import org.apache.commons.lang3.tuple.ImmutablePair;
import upc.com.pe.backendplannia.iam.domain.model.aggregates.User;
import upc.com.pe.backendplannia.iam.domain.model.commands.DeleteUserCommand;
import upc.com.pe.backendplannia.iam.domain.model.commands.SignInCommand;
import upc.com.pe.backendplannia.iam.domain.model.commands.SignUpCommand;
import upc.com.pe.backendplannia.iam.domain.model.commands.UpdateUserCommand;

import java.util.Optional;

public interface UserCommandService {
    Optional<User> handle(SignUpCommand command);
    Optional<ImmutablePair<User, String>> handle(SignInCommand command);
    Optional<User> handle(DeleteUserCommand command);
    Optional<User> handle(UpdateUserCommand command);
}
