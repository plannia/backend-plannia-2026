package upc.com.pe.backendplannia.shared.domain.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

/**
 * Base class for all entities that require auditing.
 *
 * @summary The class is an abstract mapped superclass that adds identity and auditing fields to entities.
 */
@Getter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class AuditableModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    @JsonIgnore
    private Date createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    @JsonIgnore
    private Date updatedAt;
}
