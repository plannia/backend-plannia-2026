package upc.com.pe.backendplannia.project.interfaces.rest.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import upc.com.pe.backendplannia.project.application.internal.gantt.GanttChartIntegrationException;
import upc.com.pe.backendplannia.project.domain.model.commands.CreateCategoryGanttCommand;
import upc.com.pe.backendplannia.project.domain.model.commands.DeleteCategoryMemberCommand;
import upc.com.pe.backendplannia.project.domain.services.GanttChartCommandService;
import upc.com.pe.backendplannia.project.domain.model.queries.GetAllCategoriesQuery;
import upc.com.pe.backendplannia.project.domain.services.CategoryCommandService;
import upc.com.pe.backendplannia.project.domain.services.CategoryQueryService;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.AddCategoryMemberResource;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.CategoryResource;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.CreateCategoryResource;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.UpdateCategoryResource;
import upc.com.pe.backendplannia.project.interfaces.rest.transform.AddCategoryMemberCommandFromResourceAssembler;
import upc.com.pe.backendplannia.project.interfaces.rest.transform.CategoryResourceFromEntityAssembler;
import upc.com.pe.backendplannia.project.interfaces.rest.transform.CreateCategoryCommandFromResourceAssembler;
import upc.com.pe.backendplannia.project.interfaces.rest.transform.UpdateCategoryCommandFromResourceAssembler;
import upc.com.pe.backendplannia.shared.interfaces.rest.resources.MessageResource;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/categories", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Categories", description = "Available Category Endpoints")
public class CategoryController {
    private final CategoryCommandService categoryCommandService;
    private final CategoryQueryService categoryQueryService;
    private final GanttChartCommandService ganttChartCommandService;

    public CategoryController(
            CategoryCommandService categoryCommandService,
            CategoryQueryService categoryQueryService,
            @Autowired(required = false) GanttChartCommandService ganttChartCommandService
    ) {
        this.categoryCommandService = categoryCommandService;
        this.categoryQueryService = categoryQueryService;
        this.ganttChartCommandService = ganttChartCommandService;
    }

    @GetMapping("/teams/{teamId}")
    @Operation(summary = "Get all categories by team id")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Categories found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CategoryResource.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResource.class)
                    )
            )
    })
    public ResponseEntity<List<CategoryResource>> getAllCategoriesByTeamId(@PathVariable Long teamId) {
        var categories = categoryQueryService.handle(new GetAllCategoriesQuery(teamId));
        var resources = categories.stream()
                .map(CategoryResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @PostMapping
    @Operation(summary = "Create category")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Category created",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryResource.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResource.class)
                    )
            )
    })
    public ResponseEntity<CategoryResource> createCategory(@RequestBody CreateCategoryResource resource) {
        var command = CreateCategoryCommandFromResourceAssembler.toCommandFromResource(resource);
        var category = categoryCommandService.handle(command)
                .orElseThrow(() -> new IllegalArgumentException("Category could not be created"));
        var categoryResource = CategoryResourceFromEntityAssembler.toResourceFromEntity(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryResource);
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "Update category")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Category updated",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryResource.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResource.class)
                    )
            )
    })
    public ResponseEntity<CategoryResource> updateCategory(
            @PathVariable Long categoryId,
            @RequestBody UpdateCategoryResource resource
    ) {
        var command = UpdateCategoryCommandFromResourceAssembler.toCommandFromResource(categoryId, resource);
        var category = categoryCommandService.handle(command);
        if (category.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(CategoryResourceFromEntityAssembler.toResourceFromEntity(category.get()));
    }

    @PostMapping("/{categoryId}/members")
    @Operation(summary = "Add member to category")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Member added to category",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryResource.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResource.class)
                    )
            )
    })
    public ResponseEntity<CategoryResource> addCategoryMember(
            @PathVariable Long categoryId,
            @RequestBody AddCategoryMemberResource resource
    ) {
        var command = AddCategoryMemberCommandFromResourceAssembler.toCommandFromResource(categoryId, resource);
        var category = categoryCommandService.handle(command);
        if (category.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(CategoryResourceFromEntityAssembler.toResourceFromEntity(category.get()));
    }

    @DeleteMapping("/{categoryId}/members/{userId}")
    @Operation(summary = "Remove member from category")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Member removed from category",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryResource.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResource.class)
                    )
            )
    })
    public ResponseEntity<CategoryResource> deleteCategoryMember(
            @PathVariable Long categoryId,
            @PathVariable Long userId
    ) {
        var category = categoryCommandService.handle(new DeleteCategoryMemberCommand(categoryId, userId));
        if (category.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(CategoryResourceFromEntityAssembler.toResourceFromEntity(category.get()));
    }

    @PostMapping("/{categoryId}/gantt")
    @Operation(summary = "Create or retrieve Gantt chart Google Sheet for category")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Gantt chart created or retrieved",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryResource.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResource.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Google Sheets integration failure",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResource.class)
                    )
            )
    })
    public ResponseEntity<?> createCategoryGantt(@PathVariable Long categoryId) {
        if (ganttChartCommandService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new MessageResource("La función Gantt está deshabilitada temporalmente."));
        }
        var category = ganttChartCommandService.handle(new CreateCategoryGanttCommand(categoryId));
        if (category.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(CategoryResourceFromEntityAssembler.toResourceFromEntity(category.get()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResource> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new MessageResource(exception.getMessage()));
    }

    @ExceptionHandler(GanttChartIntegrationException.class)
    public ResponseEntity<MessageResource> handleGanttIntegration(GanttChartIntegrationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new MessageResource(exception.getMessage()));
    }
}
