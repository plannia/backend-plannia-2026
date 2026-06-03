package upc.com.pe.backendplannia.iam.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.iam.domain.model.queries.GetUserByIdQuery;
import upc.com.pe.backendplannia.iam.domain.model.readmodels.UserContactReadModel;
import upc.com.pe.backendplannia.iam.domain.model.readmodels.UserDetailReadModel;
import upc.com.pe.backendplannia.iam.domain.services.MemberProfileLookupPort;
import upc.com.pe.backendplannia.iam.domain.services.UserQueryService;
import upc.com.pe.backendplannia.iam.domain.services.UserTaskStatisticsPort;
import upc.com.pe.backendplannia.iam.infrastructure.persistence.jpa.repositories.UserRepository;

import java.util.Optional;

@Service
public class UserQueryServiceImpl implements UserQueryService {
    private final UserRepository userRepository;
    private final MemberProfileLookupPort memberProfileLookupPort;
    private final UserTaskStatisticsPort userTaskStatisticsPort;

    public UserQueryServiceImpl(
            UserRepository userRepository,
            MemberProfileLookupPort memberProfileLookupPort,
            UserTaskStatisticsPort userTaskStatisticsPort
    ) {
        this.userRepository = userRepository;
        this.memberProfileLookupPort = memberProfileLookupPort;
        this.userTaskStatisticsPort = userTaskStatisticsPort;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findTeamIdByUserId(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getTeam().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findNameById(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserContactReadModel> findContactById(Long userId) {
        return userRepository.findById(userId)
                .map(user -> new UserContactReadModel(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDetailReadModel> handle(GetUserByIdQuery query) {
        return userRepository.findById(query.userId())
                .map(user -> new UserDetailReadModel(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getPosition(),
                        user.getRole(),
                        user.getTeam().getId(),
                        memberProfileLookupPort.findByUserId(user.getId()).orElse(null),
                        userTaskStatisticsPort.countByLatestAssignmentUserId(user.getId())
                ));
    }
}
