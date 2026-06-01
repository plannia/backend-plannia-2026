package upc.com.pe.backendplannia.project.interfaces.rest.transform;

import upc.com.pe.backendplannia.project.domain.model.aggregates.Category;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.CategoryResource;

import java.util.List;

public class CategoryResourceFromEntityAssembler {
    public static CategoryResource toResourceFromEntity(Category category) {
        var members = category.getMembers();
        List<Long> memberIds = members == null || members.isEmpty()
                ? null
                : members.stream().map(member -> member.id()).toList();

        return new CategoryResource(
                category.getId(),
                category.getTeamId().id(),
                category.getName(),
                category.getLimitDate(),
                category.getStatus(),
                memberIds
        );
    }
}
