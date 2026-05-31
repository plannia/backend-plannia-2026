package upc.com.pe.backendplannia.assignment.interfaces.rest;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import upc.com.pe.backendplannia.assignment.domain.model.commands.CompleteAssignmentCommand;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetAssignmentsByUserIdQuery;
import upc.com.pe.backendplannia.assignment.domain.model.queries.GetTopCandidatesQuery;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentCommandService;
import upc.com.pe.backendplannia.assignment.domain.services.AssignmentQueryService;
import upc.com.pe.backendplannia.assignment.interfaces.rest.resources.AssignmentResource;
import upc.com.pe.backendplannia.assignment.interfaces.rest.resources.CandidateProfileResource;
import upc.com.pe.backendplannia.assignment.interfaces.rest.resources.ConfirmRecommendationResource;
import upc.com.pe.backendplannia.assignment.interfaces.rest.transform.AssignmentResourceFromEntityAssembler;
import upc.com.pe.backendplannia.assignment.interfaces.rest.transform.CandidateProfileResourceFromReadModelAssembler;
import upc.com.pe.backendplannia.assignment.interfaces.rest.transform.ConfirmRecommendationCommandFromResourceAssembler;
import upc.com.pe.backendplannia.shared.interfaces.rest.resources.MessageResource;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/assignments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Assignments", description = "Available Assignment Endpoints")
public class AssignmentController {
    private final AssignmentCommandService assignmentCommandService;
    private final AssignmentQueryService assignmentQueryService;

    public AssignmentController(
            AssignmentCommandService assignmentCommandService,
            AssignmentQueryService assignmentQueryService
    ) {
        this.assignmentCommandService = assignmentCommandService;
        this.assignmentQueryService = assignmentQueryService;
    }

    @PostMapping("/recommend/confirm")
    @Operation(summary = "Confirm assignment recommendation")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Recommendation confirmed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AssignmentResource.class)
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
    public ResponseEntity<?> confirmRecommendation(@RequestBody ConfirmRecommendationResource resource) {
        try {
            var command = ConfirmRecommendationCommandFromResourceAssembler.toCommandFromResource(resource);
            var assignment = assignmentCommandService.handle(command);
            if (assignment.isEmpty()) return ResponseEntity.badRequest().build();
            var assignmentResource = AssignmentResourceFromEntityAssembler.toResourceFromEntity(assignment.get());
            return new ResponseEntity<>(assignmentResource, HttpStatus.CREATED);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new MessageResource(e.getMessage()));
        }
    }

    @GetMapping("/recommend/{taskId}/team/{teamId}")
    @Operation(summary = "Get top three assignment candidates")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Candidates found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CandidateProfileResource.class))
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Candidates not found"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResource.class)
                    )
            )
    })
    public ResponseEntity<?> getTopCandidates(
            @PathVariable Long taskId,
            @PathVariable Long teamId
    ) {
        try {
            var candidates = assignmentQueryService.handle(new GetTopCandidatesQuery(taskId, teamId));
            if (candidates.isEmpty()) return ResponseEntity.notFound().build();
            var resources = candidates.stream()
                    .map(CandidateProfileResourceFromReadModelAssembler::toResourceFromReadModel)
                    .toList();
            return ResponseEntity.ok(resources);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new MessageResource(e.getMessage()));
        }
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get assignments by user id")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Assignments found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = AssignmentResource.class))
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Assignments not found")
    })
    public ResponseEntity<List<AssignmentResource>> getAssignmentsByUserId(@PathVariable Long userId) {
        var assignments = assignmentQueryService.handle(new GetAssignmentsByUserIdQuery(userId));
        if (assignments.isEmpty()) return ResponseEntity.notFound().build();
        var resources = assignments.stream()
                .map(AssignmentResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @PatchMapping("/complete/{taskId}")
    @Operation(summary = "Complete assignment by task id")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Assignment completed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AssignmentResource.class)
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
    public ResponseEntity<?> completeAssignment(@PathVariable Long taskId) {
        try {
            var assignment = assignmentCommandService.handle(new CompleteAssignmentCommand(taskId));
            if (assignment.isEmpty()) return ResponseEntity.notFound().build();
            var resource = AssignmentResourceFromEntityAssembler.toResourceFromEntity(assignment.get());
            return ResponseEntity.ok(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResource(e.getMessage()));
        }
    }
}
