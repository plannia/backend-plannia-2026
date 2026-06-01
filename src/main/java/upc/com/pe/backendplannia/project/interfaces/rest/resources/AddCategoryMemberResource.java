package upc.com.pe.backendplannia.project.interfaces.rest.resources;

public record AddCategoryMemberResource(Long userId) {
    public AddCategoryMemberResource {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
    }
}
