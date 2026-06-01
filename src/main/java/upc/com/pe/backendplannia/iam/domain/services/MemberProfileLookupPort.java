package upc.com.pe.backendplannia.iam.domain.services;

import upc.com.pe.backendplannia.iam.domain.model.readmodels.MemberProfileSnapshot;

import java.util.Optional;

public interface MemberProfileLookupPort {
    Optional<MemberProfileSnapshot> findByUserId(Long userId);
}
