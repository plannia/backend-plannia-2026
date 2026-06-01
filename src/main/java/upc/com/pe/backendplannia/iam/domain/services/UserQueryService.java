package upc.com.pe.backendplannia.iam.domain.services;

import upc.com.pe.backendplannia.iam.domain.model.queries.GetUserByIdQuery;
import upc.com.pe.backendplannia.iam.domain.model.readmodels.UserDetailReadModel;

import java.util.Optional;

public interface UserQueryService {
    boolean existsById(Long userId);

    Optional<Long> findTeamIdByUserId(Long userId);

    Optional<String> findNameById(Long userId);

    Optional<UserDetailReadModel> handle(GetUserByIdQuery query);
}
