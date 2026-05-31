package upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import upc.com.pe.backendplannia.profile.domain.model.aggregates.MemberProfile;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {
    Optional<MemberProfile> findByUserId(Long userId);

    List<MemberProfile> findByTeamId(Long teamId);

    boolean existsByUserId(Long userId);
}
