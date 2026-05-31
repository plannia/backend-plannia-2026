package upc.com.pe.backendplannia.profile.application.internal.commandservices;

import org.junit.jupiter.api.Test;
import upc.com.pe.backendplannia.profile.domain.model.aggregates.MemberProfile;
import upc.com.pe.backendplannia.profile.domain.model.commands.AddExperienceEntryCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.CreateMemberProfileCommand;
import upc.com.pe.backendplannia.profile.domain.model.entities.ExperienceEntry;
import upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories.ExperienceEntryRepository;
import upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories.MemberProfileRepository;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExperienceEntryCommandServiceImplTests {
    private static final Long USER_ID = 101L;
    private static final Long TEAM_ID = 301L;
    private static final Long TASK_ID = 501L;

    private final ExperienceEntryRepository experienceEntryRepository = mock(ExperienceEntryRepository.class);
    private final MemberProfileRepository memberProfileRepository = mock(MemberProfileRepository.class);
    private final ExperienceEntryCommandServiceImpl service =
            new ExperienceEntryCommandServiceImpl(experienceEntryRepository, memberProfileRepository);

    @Test
    void handleSavesEntryAndRecomputesAverageExperienceEmbedding() {
        var profile = newProfile();
        when(experienceEntryRepository.save(any(ExperienceEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(memberProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        // El promedio se calcula sobre TODAS las entradas del miembro, no solo la nueva.
        when(experienceEntryRepository.findByUserId(USER_ID)).thenReturn(List.of(
                new ExperienceEntry(USER_ID, TASK_ID, EmbeddingVector.of(List.of(1f, 0f, 0f))),
                new ExperienceEntry(USER_ID, 502L, EmbeddingVector.of(List.of(0f, 1f, 0f)))
        ));

        var result = service.handle(new AddExperienceEntryCommand(
                USER_ID, TASK_ID, EmbeddingVector.of(List.of(1f, 0f, 0f))));

        assertThat(result).isPresent();
        assertThat(result.get().getTaskEmbedding().values()).containsExactly(1f, 0f, 0f);
        // (1,0,0) + (0,1,0) promediados = (0.5, 0.5, 0)
        assertThat(profile.getEmbeddedExperience().values()).containsExactly(0.5f, 0.5f, 0f);
        verify(memberProfileRepository).save(profile);
    }

    @Test
    void handleThrowsWhenMemberProfileNotFound() {
        when(experienceEntryRepository.save(any(ExperienceEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(memberProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(new AddExperienceEntryCommand(
                USER_ID, TASK_ID, EmbeddingVector.of(List.of(1f, 0f, 0f)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Member profile with this user id not found");
        verify(memberProfileRepository, org.mockito.Mockito.never()).save(any(MemberProfile.class));
    }

    private MemberProfile newProfile() {
        return new MemberProfile(new CreateMemberProfileCommand(
                USER_ID,
                TEAM_ID,
                8f,
                "Spring Boot, REST APIs, JPA",
                "Backend development and API design"
        ));
    }
}
