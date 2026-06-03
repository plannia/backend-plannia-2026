package upc.com.pe.backendplannia.notifications.domain.services;

import upc.com.pe.backendplannia.notifications.domain.model.valueobjects.UserContactSnapshot;

import java.util.Optional;

// Puerto (ACL) hacia IAM. La implementación vive en infrastructure/acl y es el ÚNICO punto
// autorizado a importar tipos del contexto IAM.
public interface UserContactPort {
    Optional<UserContactSnapshot> findByUserId(Long userId);
}
