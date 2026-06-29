package upc.com.pe.backendplannia.project.application.internal.commandservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Category;
import upc.com.pe.backendplannia.project.domain.model.commands.AddCategoryMemberCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateCategoryCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.DeleteCategoryMemberCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.RemoveCategoryMemberFromAllCategoriesCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.UpdateCategoryCommand;
import upc.com.pe.backendplannia.project.domain.model.valueobjects.UserId;
import upc.com.pe.backendplannia.project.domain.services.AssignmentActivityPort;
import upc.com.pe.backendplannia.project.domain.services.CategoryCommandService;
import upc.com.pe.backendplannia.project.domain.services.TeamExistencePort;
import upc.com.pe.backendplannia.project.domain.services.TeamMemberPort;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories.CategoryRepository;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories.TaskRepository;

import java.util.Optional;

@Service
public class CategoryCommandServiceImpl implements CategoryCommandService {
    private final CategoryRepository categoryRepository;
    private final TaskRepository taskRepository;
    private final TeamExistencePort teamExistencePort;
    private final TeamMemberPort teamMemberPort;
    private final AssignmentActivityPort assignmentActivityPort;

    public CategoryCommandServiceImpl(
            CategoryRepository categoryRepository,
            TaskRepository taskRepository,
            TeamExistencePort teamExistencePort,
            TeamMemberPort teamMemberPort,
            AssignmentActivityPort assignmentActivityPort
    ) {
        this.categoryRepository = categoryRepository;
        this.taskRepository = taskRepository;
        this.teamExistencePort = teamExistencePort;
        this.teamMemberPort = teamMemberPort;
        this.assignmentActivityPort = assignmentActivityPort;
    }

    @Override
    @Transactional
    public Optional<Category> handle(CreateCategoryCommand command) {
        if (!teamExistencePort.existsById(command.teamId())) {
            throw new IllegalArgumentException("Team not found");
        }

        var category = new Category(command);
        return Optional.of(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public Optional<Category> handle(AddCategoryMemberCommand command) {
        var category = categoryRepository.findById(command.categoryId()).orElse(null);
        if (category == null) {
            return Optional.empty();
        }

        if (!teamMemberPort.existsById(command.userId())) {
            throw new IllegalArgumentException("User not found");
        }

        var userTeamId = teamMemberPort.findTeamIdByUserId(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!category.getTeamId().id().equals(userTeamId)) {
            throw new IllegalArgumentException("User does not belong to the category team");
        }

        category.addMember(new UserId(command.userId()));
        return Optional.of(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public Optional<Category> handle(DeleteCategoryMemberCommand command) {
        var category = categoryRepository.findById(command.categoryId()).orElse(null);
        if (category == null) {
            return Optional.empty();
        }

        var memberId = new UserId(command.userId());
        if (!category.hasMember(memberId)) {
            throw new IllegalArgumentException("User is not a member of this category");
        }

        category.removeMember(memberId);
        var savedCategory = categoryRepository.save(category);

        taskRepository.findByCategory_Id(command.categoryId()).forEach(task ->
                assignmentActivityPort.findLatestAssignmentUserId(task.getId())
                        .filter(userId -> userId.equals(command.userId()))
                        .ifPresent(userId -> {
                            task.markUnassigned();
                            taskRepository.save(task);
                        })
        );

        return Optional.of(savedCategory);
    }

    @Override
    @Transactional
    public void handle(RemoveCategoryMemberFromAllCategoriesCommand command) {
        var userId = new UserId(command.userId());
        categoryRepository.findByMembers_Id(command.userId()).forEach(category -> {
            category.removeMember(userId);
            categoryRepository.save(category);
        });
    }

    @Override
    @Transactional
    public Optional<Category> handle(UpdateCategoryCommand command) {
        var category = categoryRepository.findById(command.id()).orElse(null);
        if (category == null) {
            return Optional.empty();
        }

        category.update(command);
        var savedCategory = categoryRepository.save(category);
        // Inicializa 'members' en la instancia RETORNADA, dentro de la sesión: el assembler de la
        // respuesta la lee (memberIds) y, a diferencia de add/remove member, update() nunca toca la
        // colección, por lo que quedaría como proxy lazy y reventaría al serializar fuera de la
        // transacción (LazyInitializationException).
        savedCategory.getMembers().size();
        return Optional.of(savedCategory);
    }
}
