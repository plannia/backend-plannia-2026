package upc.com.pe.backendplannia.profile.application.internal.commandservices;

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
        var existingEntry = experienceEntryRepository.findByUserIdAndTaskId(command.userId(), command.taskId());
        if (existingEntry.isPresent()) {
            return existingEntry;
        }

        var experienceEntry = new ExperienceEntry(command.userId(), command.taskId(), command.taskEmbedding());
        var savedExperienceEntry = experienceEntryRepository.save(experienceEntry);

        var memberProfile = memberProfileRepository.findByUserId(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("Member profile with this user id not found"));
        List<EmbeddingVector> experienceVectors = experienceEntryRepository.findByUserId(command.userId()).stream()
                .map(ExperienceEntry::getTaskEmbedding)
                .toList();
        memberProfile.updateExperienceEmbedding(EmbeddingVector.average(experienceVectors));
        memberProfileRepository.save(memberProfile);

        return Optional.of(savedExperienceEntry);
    }
}
