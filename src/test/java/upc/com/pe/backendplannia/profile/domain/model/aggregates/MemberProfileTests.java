package upc.com.pe.backendplannia.profile.domain.model.aggregates;

import org.junit.jupiter.api.Test;
import upc.com.pe.backendplannia.profile.domain.model.commands.CreateMemberProfileCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberProfileTests {
    private static final Long USER_ID = 101L;
    private static final Long TEAM_ID = 301L;

    @Test
    void newProfileStartsWithZeroActiveHoursAndEmptyEmbeddings() {
        var profile = newProfile(8f);

        assertThat(profile.getActiveHours()).isZero();
        assertThat(profile.getEmbeddedAbilities().dimension()).isZero();
        assertThat(profile.getEmbeddedInterests().dimension()).isZero();
        assertThat(profile.getEmbeddedExperience().dimension()).isZero();
    }

    @Test
    void isAvailableReflectsRemainingCapacity() {
        var profile = newProfile(8f);
        profile.updateActiveHours(5f);

        assertThat(profile.isAvailable(3f)).isTrue();
        assertThat(profile.isAvailable(4f)).isFalse();
    }

    @Test
    void updateActiveHoursAccumulates() {
        var profile = newProfile(8f);

        profile.updateActiveHours(3f);
        profile.updateActiveHours(2f);

        assertThat(profile.getActiveHours()).isEqualTo(5f);
    }

    @Test
    void updateActiveHoursRejectsNonPositive() {
        var profile = newProfile(8f);

        assertThatThrownBy(() -> profile.updateActiveHours(0f))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Hours must be greater than zero");
    }

    @Test
    void updateActiveHoursRejectsExceedingCapacity() {
        var profile = newProfile(8f);

        assertThatThrownBy(() -> profile.updateActiveHours(9f))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Active hours cannot exceed max hours");
        assertThat(profile.getActiveHours()).isZero();
    }

    @Test
    void reduceActiveHoursReducesCurrentLoad() {
        var profile = newProfile(8f);
        profile.updateActiveHours(5f);

        profile.reduceActiveHours(2f);

        assertThat(profile.getActiveHours()).isEqualTo(3f);
    }

    @Test
    void reduceActiveHoursRejectsNonPositive() {
        var profile = newProfile(8f);

        assertThatThrownBy(() -> profile.reduceActiveHours(0f))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Hours must be greater than zero");
    }

    @Test
    void reduceActiveHoursRejectsNegativeResult() {
        var profile = newProfile(8f);
        profile.updateActiveHours(2f);

        assertThatThrownBy(() -> profile.reduceActiveHours(3f))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Active hours cannot be negative");
    }

    @Test
    void updateMaxHoursRejectsNonPositive() {
        var profile = newProfile(8f);

        assertThatThrownBy(() -> profile.updateMaxHours(0f))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Max hours must be greater than zero");
    }

    @Test
    void updateMaxHoursRejectsValueBelowActiveHours() {
        var profile = newProfile(8f);
        profile.updateActiveHours(5f);

        assertThatThrownBy(() -> profile.updateMaxHours(4f))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Max hours cannot be lower than active hours");
    }

    @Test
    void updateMaxHoursUpdatesCapacity() {
        var profile = newProfile(8f);

        profile.updateMaxHours(10f);

        assertThat(profile.getMaxHours()).isEqualTo(10f);
    }

    private MemberProfile newProfile(float maxHours) {
        return new MemberProfile(new CreateMemberProfileCommand(
                USER_ID,
                TEAM_ID,
                maxHours,
                "Spring Boot, REST APIs, JPA",
                "Backend development and API design"
        ));
    }
}
