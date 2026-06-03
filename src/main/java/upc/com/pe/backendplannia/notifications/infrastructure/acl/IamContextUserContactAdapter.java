package upc.com.pe.backendplannia.notifications.infrastructure.acl;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.iam.domain.model.valueobjects.Role;
import upc.com.pe.backendplannia.iam.domain.services.UserQueryService;
import upc.com.pe.backendplannia.notifications.domain.model.valueobjects.UserContactSnapshot;
import upc.com.pe.backendplannia.notifications.domain.services.UserContactPort;

import java.util.Optional;

/**
 * Anti-Corruption Layer: ÚNICO punto de Notifications que conoce tipos de IAM.
 * Traduce el UserContactReadModel (y su Role) al UserContactSnapshot propio de Notifications.
 */
@Service
public class IamContextUserContactAdapter implements UserContactPort {
    private final UserQueryService userQueryService;

    public IamContextUserContactAdapter(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @Override
    public Optional<UserContactSnapshot> findByUserId(Long userId) {
        return userQueryService.findContactById(userId)
                .map(contact -> new UserContactSnapshot(
                        contact.id(),
                        contact.name(),
                        contact.email(),
                        contact.role() == Role.MEMBER
                ));
    }
}
