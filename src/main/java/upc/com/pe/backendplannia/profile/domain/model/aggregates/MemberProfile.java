package upc.com.pe.backendplannia.profile.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import lombok.Getter;
import upc.com.pe.backendplannia.profile.domain.model.commands.CreateMemberProfileCommand;
import upc.com.pe.backendplannia.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;
import upc.com.pe.backendplannia.shared.infrastructure.persistence.jpa.converters.EmbeddingVectorConverter;

import java.util.List;

@Entity
@Getter
public class MemberProfile extends AuditableAbstractAggregateRoot<MemberProfile> {
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long teamId;

    @Column(nullable = false)
    private float maxHours;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String abilities;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String interests;

    @Column(nullable = false)
    private float activeHours;

    @Convert(converter = EmbeddingVectorConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private EmbeddingVector embeddedAbilities;

    @Convert(converter = EmbeddingVectorConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private EmbeddingVector embeddedInterests;

    @Convert(converter = EmbeddingVectorConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private EmbeddingVector embeddedExperience;

    protected MemberProfile() {
    }

    public MemberProfile(CreateMemberProfileCommand command) {
        this.userId = command.userId();
        this.teamId = command.teamId();
        this.maxHours = command.maxHours();
        this.abilities = command.abilities();
        this.interests = command.interests();
        this.activeHours = 0;
        this.embeddedAbilities = EmbeddingVector.of(List.of());
        this.embeddedInterests = EmbeddingVector.of(List.of());
        this.embeddedExperience = EmbeddingVector.of(List.of());
    }

    public boolean isAvailable(float requiredHours) {
        return (maxHours - activeHours) >= requiredHours;
    }

    public void updateMaxHours(float maxHours) {
        if (maxHours <= 0) {
            throw new IllegalArgumentException("Max hours must be greater than zero");
        }
        if (maxHours < activeHours) {
            throw new IllegalArgumentException("Max hours cannot be lower than active hours");
        }
        this.maxHours = maxHours;
    }

    public void updateAbilities(String abilities) {
        this.abilities = abilities;
    }

    public void updateInterests(String interests) {
        this.interests = interests;
    }

    public void updateExperienceEmbedding(EmbeddingVector experience) {
        this.embeddedExperience = experience;
    }

    public void updateInterestEmbedding(EmbeddingVector interests) {
        this.embeddedInterests = interests;
    }

    public void updateAbilityEmbedding(EmbeddingVector abilities) {
        this.embeddedAbilities = abilities;
    }

    // Las horas se manejan como float para alinearse con maxHours/activeHours; estimatedHours (int)
    // de una tarea se ensancha automáticamente al llamar a estos métodos.
    public void updateActiveHours(float hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Hours must be greater than zero");
        }
        if (!isAvailable(hours)) {
            throw new IllegalArgumentException("Active hours cannot exceed max hours");
        }
        this.activeHours += hours;
    }

    public void reduceActiveHours(float hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Hours must be greater than zero");
        }
        if (activeHours - hours < 0) {
            throw new IllegalArgumentException("Active hours cannot be negative");
        }
        this.activeHours -= hours;
    }
}
