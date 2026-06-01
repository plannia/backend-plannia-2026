package upc.com.pe.backendplannia.project.domain.services;

import upc.com.pe.backendplannia.project.domain.model.aggregates.Category;
import upc.com.pe.backendplannia.project.domain.model.commands.AddCategoryMemberCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateCategoryCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.DeleteCategoryMemberCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.RemoveCategoryMemberFromAllCategoriesCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.UpdateCategoryCommand;

import java.util.Optional;

public interface CategoryCommandService {
    Optional<Category> handle(CreateCategoryCommand command);

    Optional<Category> handle(AddCategoryMemberCommand command);

    Optional<Category> handle(DeleteCategoryMemberCommand command);

    void handle(RemoveCategoryMemberFromAllCategoriesCommand command);

    Optional<Category> handle(UpdateCategoryCommand command);
}