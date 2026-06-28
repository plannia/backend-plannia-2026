package upc.com.pe.backendplannia.profile.application.internal.commandservices;

import org.junit.jupiter.api.Test;
import upc.com.pe.backendplannia.profile.application.internal.outboundservices.ai.ProfileEmbeddingService;
import upc.com.pe.backendplannia.profile.domain.model.aggregates.MemberProfile;
import upc.com.pe.backendplannia.profile.domain.model.commands.CreateMemberProfileCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.UpdateMemberProfileCommand;
import upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories.ExperienceEntryRepository;
import upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories.MemberProfileRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateMemberProfileCommandServiceTests {
    private static final Long USER_ID = 101L;
    private static final Long TEAM_ID = 301L;

    private final MemberProfileRepository repository = mock(MemberProfileRepository.class);
    private final ProfileEmbeddingService embeddingService = mock(ProfileEmbeddingService.class);
    private final ExperienceEntryRepository experienceEntryRepository = mock(ExperienceEntryRepository.class);
    private final MemberProfileCommandServiceImpl service =
            new MemberProfileCommandServiceImpl(repository, embeddingService, experienceEntryRepository);

    @Test
    void updatesOnlyMaxHoursWhenAbilitiesAndInterestsAreNull() {
        var profile = new MemberProfile(new CreateMemberProfileCommand(USER_ID, TEAM_ID, 40f, "", ""));
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        when(repository.save(any(MemberProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.handle(new UpdateMemberProfileCommand(USER_ID, 35f, null, null));

        assertThat(result).isPresent();
        assertThat(result.get().getMaxHours()).isEqualTo(35f);
        verify(embeddingService, never()).generateEmbedding(any());
    }

    @Test
    void returnsEmptyWhenProfileDoesNotExist() {
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        var result = service.handle(new UpdateMemberProfileCommand(USER_ID, 35f, "Java", "Spring"));

        assertThat(result).isEmpty();
        verify(repository, never()).save(any());
    }
}
