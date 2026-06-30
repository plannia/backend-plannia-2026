package upc.com.pe.backendplannia.project.interfaces.rest.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import upc.com.pe.backendplannia.project.domain.model.queries.GetTasksByFilterQuery;
import upc.com.pe.backendplannia.project.domain.model.queries.GetTasksForDashboardQuery;
import upc.com.pe.backendplannia.project.domain.model.queries.GetTasksForPlannerQuery;
import upc.com.pe.backendplannia.project.domain.services.TaskCommandService;
import upc.com.pe.backendplannia.project.domain.services.TaskQueryService;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.CreateTaskResource;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.DashboardTaskResource;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.TaskResource;
import upc.com.pe.backendplannia.project.interfaces.rest.resources.UpdateTaskResource;
import upc.com.pe.backendplannia.project.interfaces.rest.transform.CreateTaskCommandFromResourceAssembler;
import upc.com.pe.backendplannia.project.interfaces.rest.transform.DashboardTaskResourceFromReadModelAssembler;
import upc.com.pe.backendplannia.project.interfaces.rest.transform.TaskResourceFromEntityAssembler;
import upc.com.pe.backendplannia.project.interfaces.rest.transform.UpdateTaskCommandFromResourceAssembler;
import upc.com.pe.backendplannia.shared.interfaces.rest.resources.MessageResource;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tasks", description = "Available Task Endpoints")
public class TaskController {
    private final TaskCommandService taskCommandService;
    private final TaskQueryService taskQueryService;

    public TaskController(TaskCommandService taskCommandService, TaskQueryService taskQueryService) {
        this.taskCommandService = taskCommandService;
        this.taskQueryService = taskQueryService;
    }

    @GetMapping("/teams/{teamId}")
    @Operation(summary = "Get tasks by team id and optional filters")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tasks found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = TaskResource.class))
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
    public ResponseEntity<List<TaskResource>> getTasksByFilter(
            @PathVariable Long teamId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String userId
    ) {
        var query = new GetTasksByFilterQuery(
                teamId,
                title,
                description,
                priority,
                difficulty,
                status,
                categoryId,
                userId
        );
        var tasks = taskQueryService.handle(query);
        var resources = tasks.stream()
                .map(TaskResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/dashboard/teams/{teamId}")
    @Operation(summary = "Get in-progress assigned tasks for dashboard")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Dashboard tasks found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = DashboardTaskResource.class))
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
    public ResponseEntity<List<DashboardTaskResource>> getTasksForDashboard(@PathVariable Long teamId) {
        var items = taskQueryService.handle(new GetTasksForDashboardQuery(teamId));
        var resources = items.stream()
                .map(DashboardTaskResourceFromReadModelAssembler::toResourceFromReadModel)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/planner/teams/{teamId}")
    @Operation(summary = "Get in-progress and completed tasks for team planner")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Planner tasks found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = DashboardTaskResource.class))
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
    public ResponseEntity<List<DashboardTaskResource>> getTasksForPlanner(@PathVariable Long teamId) {
        var items = taskQueryService.handle(new GetTasksForPlannerQuery(teamId));
        var resources = items.stream()
                .map(DashboardTaskResourceFromReadModelAssembler::toResourceFromReadModel)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @PostMapping
    @Operation(summary = "Create task in category")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Task created",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TaskResource.class)
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
    public ResponseEntity<TaskResource> createTask(@RequestBody CreateTaskResource resource) {
        var command = CreateTaskCommandFromResourceAssembler.toCommandFromResource(resource);
        var task = taskCommandService.handle(command)
                .orElseThrow(() -> new IllegalArgumentException("Task could not be created"));
        var taskResource = TaskResourceFromEntityAssembler.toResourceFromEntity(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(taskResource);
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Update task status")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Task updated",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TaskResource.class)
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
    public ResponseEntity<TaskResource> updateTask(
            @PathVariable Long taskId,
            @RequestBody UpdateTaskResource resource
    ) {
        var command = UpdateTaskCommandFromResourceAssembler.toCommandFromResource(taskId, resource);
        var task = taskCommandService.handle(command)
                .orElseThrow(() -> new IllegalArgumentException("Task could not be updated"));
        return ResponseEntity.ok(TaskResourceFromEntityAssembler.toResourceFromEntity(task));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResource> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new MessageResource(exception.getMessage()));
    }
}
