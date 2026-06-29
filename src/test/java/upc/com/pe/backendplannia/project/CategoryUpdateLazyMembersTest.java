package upc.com.pe.backendplannia.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import upc.com.pe.backendplannia.profile.application.internal.outboundservices.ai.ProfileEmbeddingService;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Category;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateCategoryCommand;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.UserId;
import upc.com.pe.backendplannia.project.interfaces.rest.controllers.CategoryController;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.CategoryResource;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.UpdateCategoryResource;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories.CategoryRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regresión: PUT /categories/{id} reventaba con LazyInitializationException porque update() nunca toca
 * la colección 'members' y el assembler de la respuesta la lee fuera de la transacción.
 *
 * El test llama al controller (que ejecuta el assembler, igual que el flujo real) y verifica que la
 * respuesta se arma bien — incluyendo memberIds — sin lanzar.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:plannia-catupd;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "JWT_SECRET=integration-test-secret-please-32-chars",
        "notifications.email.transport=log"
})
class CategoryUpdateLazyMembersTest {

    @Autowired
    private CategoryController categoryController;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private ProfileEmbeddingService profileEmbeddingService;

    @Test
    void updateCategorySerializesMembersWithoutLazyInitializationError() {
        // Categoría con un miembro, persistida; el update posterior la recarga (members queda lazy).
        var category = new Category(new CreateCategoryCommand(25L, "Plataforma", LocalDateTime.now().plusDays(30)));
        category.addMember(new UserId(46L));
        var saved = categoryRepository.save(category);

        var response = categoryController.updateCategory(
                saved.getId(), new UpdateCategoryResource("Plataforma v2", null, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CategoryResource body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.name()).isEqualTo("Plataforma v2");
        // La lectura de memberIds es justo lo que reventaba sin el fix.
        assertThat(body.memberIds()).containsExactly(46L);
    }
}
