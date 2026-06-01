package upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Category;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByMembers_Id(Long userId);

    List<Category> findByTeamId_Id(Long teamId);
}
