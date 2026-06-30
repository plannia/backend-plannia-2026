package upc.com.pe.backendplannia.profile.application.internal.commandservices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.profile.domain.model.commands.AddExperienceEntryCommand;
import upc.com.pe.backendplannia.profile.domain.model.entities.ExperienceEntry;
import upc.com.pe.backendplannia.profile.domain.services.ExperienceEntryCommandService;
import upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories.ExperienceEntryRepository;
import upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories.MemberProfileRepository;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.List;
import java.util.Optional;

@Service
public class ExperienceEntryCommandServiceImpl implements ExperienceEntryCommandService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperienceEntryCommandServiceImpl.class);

    private final ExperienceEntryRepository experienceEntryRepository;
    private final MemberProfileRepository memberProfileRepository;

    public ExperienceEntryCommandServiceImpl(
            ExperienceEntryRepository experienceEntryRepository,
            MemberProfileRepository memberProfileRepository
    ) {
        this.experienceEntryRepository = experienceEntryRepository;
        this.memberProfileRepository = memberProfileRepository;
    }

    // Dos escrituras (la entrada + el promedio recalculado en el perfil) deben confirmar/revertir juntas.
    @Override
    @Transactional
    public Optional<ExperienceEntry> handle(AddExperienceEntryCommand command) {
        LOGGER.info(
                "Add experience entry requested: userId={}, taskId={}, embeddingDim={}",
                command.userId(),
                command.taskId(),
                command.taskEmbedding().dimension()
        );

        var existingEntry = experienceEntryRepository.findByUserIdAndTaskId(command.userId(), command.taskId());
        if (existingEntry.isPresent()) {
            LOGGER.info(
                    "Experience entry already exists; skipping insert: userId={}, taskId={}, entryId={}",
                    command.userId(),
                    command.taskId(),
                    existingEntry.get().getId()
            );
            return existingEntry;
        }

        var experienceEntry = new ExperienceEntry(command.userId(), command.taskId(), command.taskEmbedding());
        var savedExperienceEntry = experienceEntryRepository.save(experienceEntry);
        LOGGER.info(
                "Experience entry saved: userId={}, taskId={}, entryId={}",
                command.userId(),
                command.taskId(),
                savedExperienceEntry.getId()
        );

        var memberProfile = memberProfileRepository.findByUserId(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("Member profile with this user id not found"));
        List<EmbeddingVector> experienceVectors = experienceEntryRepository.findByUserId(command.userId()).stream()
                .map(ExperienceEntry::getTaskEmbedding)
                .toList();
        LOGGER.info(
                "Recalculating member experience embedding: userId={}, vectorCount={}, firstVectorDim={}",
                command.userId(),
                experienceVectors.size(),
                experienceVectors.isEmpty() ? 0 : experienceVectors.getFirst().dimension()
        );
        memberProfile.updateExperienceEmbedding(EmbeddingVector.average(experienceVectors));
        memberProfileRepository.save(memberProfile);
        LOGGER.info(
                "Member experience embedding updated: userId={}, embeddingDim={}",
                command.userId(),
                memberProfile.getEmbeddedExperience().dimension()
        );

        return Optional.of(savedExperienceEntry);
    }
}
