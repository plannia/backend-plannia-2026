package upc.com.pe.backendplannia.project.domain.model.commands;

public record DeleteCategoryMemberCommand(Long categoryId, Long userId) {
    public DeleteCategoryMemberCommand {
        if (categoryId == null || userId == null) {
            throw new IllegalArgumentException("categoryId and userId are required");
        }
    }
}
