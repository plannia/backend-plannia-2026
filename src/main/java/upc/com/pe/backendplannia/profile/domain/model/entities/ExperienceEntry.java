package upc.com.pe.backendplannia.profile.domain.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import lombok.Getter;
import upc.com.pe.backendplannia.shared.domain.model.entities.AuditableModel;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;
import upc.com.pe.backendplannia.shared.infrastructure.persistence.jpa.converters.EmbeddingVectorConverter;

@Entity
@Getter
public class ExperienceEntry extends AuditableModel {
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long taskId;

    @Convert(converter = EmbeddingVectorConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private EmbeddingVector taskEmbedding;

    protected ExperienceEntry() {}

    public ExperienceEntry(Long userId, Long taskId, EmbeddingVector taskEmbedding) {
        this.userId = userId;
        this.taskId = taskId;
        this.taskEmbedding = taskEmbedding;
    }
}
