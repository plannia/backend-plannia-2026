package upc.com.pe.backendplannia.project.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.com.pe.backendplannia.project.domain.model.aggregates.Category;
import upc.com.pe.backendplannia.project.domain.model.queries.GetAllCategoriesQuery;
import upc.com.pe.backendplannia.project.domain.services.CategoryQueryService;
import upc.com.pe.backendplannia.project.domain.services.TeamExistencePort;
import upc.com.pe.backendplannia.project.infrastructure.persistence.jpa.repositories.CategoryRepository;

import java.util.List;

@Service
public class CategoryQueryServiceImpl implements CategoryQueryService {
    private final CategoryRepository categoryRepository;
    private final TeamExistencePort teamExistencePort;

    public CategoryQueryServiceImpl(CategoryRepository categoryRepository, TeamExistencePort teamExistencePort) {
        this.categoryRepository = categoryRepository;
        this.teamExistencePort = teamExistencePort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> handle(GetAllCategoriesQuery query) {
        if (!teamExistencePort.existsById(query.teamId())) {
            throw new IllegalArgumentException("Team not found");
        }
        return categoryRepository.findByTeamId_Id(query.teamId());
    }
}
