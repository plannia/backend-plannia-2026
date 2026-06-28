package upc.com.pe.backendplannia.profile.application.internal.commandservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.profile.application.internal.outboundservices.ai.ProfileEmbeddingService;
import upc.com.pe.backendplannia.profile.domain.model.aggregates.MemberProfile;
import upc.com.pe.backendplannia.profile.domain.model.commands.CreateDefaultMemberProfileCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.CreateMemberProfileCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.DeleteMemberProfileCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.ReduceActiveHoursCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.UpdateActiveHoursCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.UpdateMaxHoursCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.UpdateMemberProfileCommand;
import upc.com.pe.backendplannia.profile.domain.services.MemberProfileCommandService;
import upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories.ExperienceEntryRepository;
import upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories.MemberProfileRepository;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.Optional;

@Service
public class MemberProfileCommandServiceImpl implements MemberProfileCommandService {
    // Jornada semanal típica; el miembro la ajusta al completar su perfil.
    private static final float DEFAULT_MAX_HOURS = 40f;

    private final MemberProfileRepository memberProfileRepository;
    private final ProfileEmbeddingService profileEmbeddingService;
    private final ExperienceEntryRepository experienceEntryRepository;

    public MemberProfileCommandServiceImpl(
            MemberProfileRepository memberProfileRepository,
            ProfileEmbeddingService profileEmbeddingService,
            ExperienceEntryRepository experienceEntryRepository
    ) {
        this.memberProfileRepository = memberProfileRepository;
        this.profileEmbeddingService = profileEmbeddingService;
        this.experienceEntryRepository = experienceEntryRepository;
    }

    @Override
    public Optional<MemberProfile> handle(CreateMemberProfileCommand command) {
        if (memberProfileRepository.existsByUserId(command.userId())) {
            throw new IllegalArgumentException("Member profile with this user id already exists");
        }

        var memberProfile = new MemberProfile(command);
        memberProfile.updateAbilityEmbedding(profileEmbeddingService.generateEmbedding(command.abilities()));
        memberProfile.updateInterestEmbedding(profileEmbeddingService.generateEmbedding(command.interests()));

        var savedMemberProfile = memberProfileRepository.save(memberProfile);
        return Optional.of(savedMemberProfile);
    }

    @Override
    public Optional<MemberProfile> handle(CreateDefaultMemberProfileCommand command) {
        // Idempotente: si ya existe (p. ej. reintento del evento), no lo dupliques.
        var existing = memberProfileRepository.findByUserId(command.userId());
        if (existing.isPresent()) {
            return existing;
        }

        // Perfil BASE: sin skills ni embeddings (no se llama a la IA con texto vacío). Se completa luego
        // con UpdateMemberProfileCommand, que sí genera los embeddings. Un perfil sin embeddings NO es
        // candidato (lo filtra el ProfileContextCandidateProfileProvider).
        var memberProfile = new MemberProfile(new CreateMemberProfileCommand(
                command.userId(), command.teamId(), DEFAULT_MAX_HOURS, "", ""));
        return Optional.of(memberProfileRepository.save(memberProfile));
    }

    // Borra todo rastro del miembro en Profile: sus entradas de experiencia y su perfil. Dos escrituras
    // → @Transactional para que confirmen/reviertan juntas. Idempotente (no falla si no hay perfil).
    @Override
    @Transactional
    public void handle(DeleteMemberProfileCommand command) {
        experienceEntryRepository.deleteByUserId(command.userId());
        memberProfileRepository.findByUserId(command.userId())
                .ifPresent(memberProfileRepository::delete);
    }

    @Override
    public Optional<MemberProfile> handle(UpdateMemberProfileCommand command) {
        var result = memberProfileRepository.findByUserId(command.userId());
        if (result.isEmpty()) {
            return Optional.empty();
        }

        var memberProfile = result.get();
        // Null means no change; only non-null fields are applied.
        if (command.maxHours() != null) {
            memberProfile.updateMaxHours(command.maxHours());
        }
        if (command.abilities() != null) {
            memberProfile.updateAbilities(command.abilities());
            memberProfile.updateAbilityEmbedding(generateEmbeddingOrThrow(command.abilities(), "abilities"));
        }
        if (command.interests() != null) {
            memberProfile.updateInterests(command.interests());
            memberProfile.updateInterestEmbedding(generateEmbeddingOrThrow(command.interests(), "interests"));
        }

        var savedMemberProfile = memberProfileRepository.save(memberProfile);
        return Optional.of(savedMemberProfile);
    }

    private EmbeddingVector generateEmbeddingOrThrow(String text, String fieldName) {
        try {
            return profileEmbeddingService.generateEmbedding(text);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Failed to generate " + fieldName + " embedding. Check AI configuration and try again.",
                    exception
            );
        }
    }

    @Override
    public Optional<MemberProfile> handle(UpdateMaxHoursCommand command) {
        var memberProfile = findByUserIdOrThrow(command.userId());
        memberProfile.updateMaxHours(command.maxHours());
        return Optional.of(memberProfileRepository.save(memberProfile));
    }

    @Override
    public Optional<MemberProfile> handle(UpdateActiveHoursCommand command) {
        var memberProfile = findByUserIdOrThrow(command.userId());
        memberProfile.updateActiveHours(command.hours());
        return Optional.of(memberProfileRepository.save(memberProfile));
    }

    @Override
    public Optional<MemberProfile> handle(ReduceActiveHoursCommand command) {
        var memberProfile = findByUserIdOrThrow(command.userId());
        memberProfile.reduceActiveHours(command.hours());
        return Optional.of(memberProfileRepository.save(memberProfile));
    }

    private MemberProfile findByUserIdOrThrow(Long userId) {
        return memberProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Member profile with this user id not found"));
    }
}
