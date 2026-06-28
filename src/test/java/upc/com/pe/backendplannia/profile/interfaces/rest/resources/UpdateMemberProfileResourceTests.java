package upc.com.pe.backendplannia.profile.interfaces.rest.resources;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateMemberProfileResourceTests {

    @Test
    void blankStringsAreTreatedAsNoChange() {
        var resource = new UpdateMemberProfileResource(35f, "", "   ");

        assertThat(resource.maxHours()).isEqualTo(35f);
        assertThat(resource.abilities()).isNull();
        assertThat(resource.interests()).isNull();
    }

    @Test
    void trimsNonBlankTextFields() {
        var resource = new UpdateMemberProfileResource(null, "  Java  ", " Spring ");

        assertThat(resource.abilities()).isEqualTo("Java");
        assertThat(resource.interests()).isEqualTo("Spring");
    }

    @Test
    void rejectsNonPositiveMaxHours() {
        assertThatThrownBy(() -> new UpdateMemberProfileResource(0f, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Max hours must be greater than zero");
    }
}
