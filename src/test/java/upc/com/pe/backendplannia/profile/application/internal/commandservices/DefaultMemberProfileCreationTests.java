package upc.com.pe.backendplannia.profile.application.internal.commandservices;

import org.junit.jupiter.api.Test;
import upc.com.pe.backendplannia.profile.application.internal.outboundservices.ai.ProfileEmbeddingService;
import upc.com.pe.backendplannia.profile.domain.model.aggregates.MemberProfile;
import upc.com.pe.backendplannia.profile.domain.model.commands.CreateDefaultMemberProfileCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.CreateMemberProfileCommand;
import upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories.ExperienceEntryRepository;
import upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories.MemberProfileRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultMemberProfileCreationTests {
    private static final Long USER_ID = 101L;
    private static final Long TEAM_ID = 301L;

    private final MemberProfileRepository repository = mock(MemberProfileRepository.class);
    private final ProfileEmbeddingService embeddingService = mock(ProfileEmbeddingService.class);
    private final ExperienceEntryRepository experienceEntryRepository = mock(ExperienceEntryRepository.class);
    private final MemberProfileCommandServiceImpl service =
            new MemberProfileCommandServiceImpl(repository, embeddingService, experienceEntryRepository);

    @Test
    void createsBaseProfileWithDefaultsAndWithoutCallingTheEmbeddingApi() {
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(repository.save(any(MemberProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.handle(new CreateDefaultMemberProfileCommand(USER_ID, TEAM_ID));

        assertThat(result).isPresent();
        assertThat(result.get().getMaxHours()).isEqualTo(40f);
        // Perfil base = sin skills ni embeddings (no se puede embeber texto vacío).
        assertThat(result.get().getEmbeddedAbilities().dimension()).isZero();
        verify(embeddingService, never()).generateEmbedding(anyString());
        verify(repository).save(any(MemberProfile.class));
    }

    @Test
    void isIdempotentWhenProfileAlreadyExists() {
        var existing = new MemberProfile(new CreateMemberProfileCommand(USER_ID, TEAM_ID, 40f, "x", "y"));
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));

        var result = service.handle(new CreateDefaultMemberProfileCommand(USER_ID, TEAM_ID));

        assertThat(result).contains(existing);
        verify(repository, never()).save(any(MemberProfile.class));
        verify(embeddingService, never()).generateEmbedding(anyString());
    }
}
