package upc.com.pe.backendplannia.assignment.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Optional<Assignment> findFirstByTaskIdOrderByCreatedAtDesc(Long taskId);

    @Query("""
            SELECT a FROM Assignment a
            WHERE a.userId = :userId
            AND a.createdAt = (
                SELECT MAX(a2.createdAt) FROM Assignment a2 WHERE a2.taskId = a.taskId
            )
            """)
    List<Assignment> findLatestAssignmentsByUserId(@Param("userId") Long userId);
}
