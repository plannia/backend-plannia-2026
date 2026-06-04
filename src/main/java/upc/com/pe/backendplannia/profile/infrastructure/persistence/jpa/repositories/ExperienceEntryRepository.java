package upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import upc.com.pe.backendplannia.profile.domain.model.entities.ExperienceEntry;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExperienceEntryRepository extends JpaRepository<ExperienceEntry, Long> {
    List<ExperienceEntry> findByUserId(Long userId);

    List<ExperienceEntry> findByTaskId(Long taskId);

    Optional<ExperienceEntry> findByUserIdAndTaskId(Long userId, Long taskId);

    void deleteByUserId(Long userId);
}
