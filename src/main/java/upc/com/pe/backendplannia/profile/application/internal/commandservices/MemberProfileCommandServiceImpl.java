package upc.com.pe.backendplannia.profile.application.internal.commandservices;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.profile.application.internal.outboundservices.ai.ProfileEmbeddingService;
import upc.com.pe.backendplannia.profile.domain.model.aggregates.MemberProfile;
import upc.com.pe.backendplannia.profile.domain.model.commands.CreateMemberProfileCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.ReduceActiveHoursCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.UpdateActiveHoursCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.UpdateMaxHoursCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.UpdateMemberProfileCommand;
import upc.com.pe.backendplannia.profile.domain.services.MemberProfileCommandService;
import upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories.MemberProfileRepository;

import java.util.Optional;

@Service
public class MemberProfileCommandServiceImpl implements MemberProfileCommandService {
    private final MemberProfileRepository memberProfileRepository;
    private final ProfileEmbeddingService profileEmbeddingService;

    public MemberProfileCommandServiceImpl(
            MemberProfileRepository memberProfileRepository,
            ProfileEmbeddingService profileEmbeddingService
    ) {
        this.memberProfileRepository = memberProfileRepository;
        this.profileEmbeddingService = profileEmbeddingService;
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
    public Optional<MemberProfile> handle(UpdateMemberProfileCommand command) {
        var result = memberProfileRepository.findByUserId(command.userId());
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Member profile with this user id not found");
        }

        var memberProfile = result.get();
        // Null means no change; only non-null fields are applied.
        if (command.maxHours() != null) {
            memberProfile.updateMaxHours(command.maxHours());
        }
        if (command.abilities() != null) {
            memberProfile.updateAbilities(command.abilities());
            memberProfile.updateAbilityEmbedding(profileEmbeddingService.generateEmbedding(command.abilities()));
        }
        if (command.interests() != null) {
            memberProfile.updateInterests(command.interests());
            memberProfile.updateInterestEmbedding(profileEmbeddingService.generateEmbedding(command.interests()));
        }

        var savedMemberProfile = memberProfileRepository.save(memberProfile);
        return Optional.of(savedMemberProfile);
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
