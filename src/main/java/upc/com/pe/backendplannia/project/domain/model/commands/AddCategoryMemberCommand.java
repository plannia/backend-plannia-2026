package upc.com.pe.backendplannia.project.domain.model.commands;

public record AddCategoryMemberCommand(Long categoryId, Long userId) {
    public AddCategoryMemberCommand {
        if (categoryId == null || userId == null) {
            throw new IllegalArgumentException("categoryId and userId are required");
        }
    }
}