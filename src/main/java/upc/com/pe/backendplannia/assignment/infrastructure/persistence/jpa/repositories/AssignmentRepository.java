package upc.com.pe.backendplannia.assignment.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import upc.com.pe.backendplannia.assignment.domain.model.aggregates.Assignment;
import upc.com.pe.backendplannia.assignment.domain.model.valueobjects.AssignmentStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByUserId(Long userId);

    List<Assignment> findByTaskId(Long taskId);

    List<Assignment> findByStatus(AssignmentStatus status);

    Optional<Assignment> findByTaskIdAndUserId(Long taskId, Long userId);

    Optional<Assignment> findByTaskIdAndStatus(Long taskId, AssignmentStatus status);

    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    boolean existsByTaskIdAndStatus(Long taskId, AssignmentStatus status);
}
