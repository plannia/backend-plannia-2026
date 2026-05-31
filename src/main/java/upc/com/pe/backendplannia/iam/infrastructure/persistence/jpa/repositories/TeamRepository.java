package upc.com.pe.backendplannia.iam.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import upc.com.pe.backendplannia.iam.domain.model.aggregates.Team;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByCode(String code);
    boolean existsByName(String name);
    Optional<Team> findByCode(String code);

    @EntityGraph(attributePaths = "users")
    Optional<Team> findWithUsersById(Long id);
}
