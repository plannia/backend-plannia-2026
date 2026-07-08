package upc.com.pe.backendplannia.profile.application.internal.queryservices;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.profile.domain.model.entities.ExperienceEntry;
import upc.com.pe.backendplannia.profile.domain.services.MemberExperienceQueryService;
import upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories.ExperienceEntryRepository;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.List;

@Service
public class MemberExperienceQueryServiceImpl implements MemberExperienceQueryService {
    private final ExperienceEntryRepository experienceEntryRepository;

    public MemberExperienceQueryServiceImpl(ExperienceEntryRepository experienceEntryRepository) {
        this.experienceEntryRepository = experienceEntryRepository;
    }

    @Override
    public List<EmbeddingVector> findExperienceEmbeddings(Long userId) {
        return experienceEntryRepository.findByUserId(userId).stream()
                .map(ExperienceEntry::getTaskEmbedding)
                .filter(embedding -> embedding != null && embedding.dimension() > 0)
                .toList();
    }
}
