package upc.com.pe.backendplannia.project.domain.model.commands;

public record RemoveCategoryMemberFromAllCategoriesCommand(Long userId) {
    public RemoveCategoryMemberFromAllCategoriesCommand {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
    }
}
