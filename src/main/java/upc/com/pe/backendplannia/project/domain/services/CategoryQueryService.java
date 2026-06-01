package upc.com.pe.backendplannia.project.domain.services;

import upc.com.pe.backendplannia.project.domain.model.aggregates.Category;
import upc.com.pe.backendplannia.project.domain.model.queries.GetAllCategoriesQuery;

import java.util.List;

public interface CategoryQueryService {
    List<Category> handle(GetAllCategoriesQuery query);
}
