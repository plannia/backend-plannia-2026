package upc.com.pe.backendplannia.iam.interfaces.rest.controllers;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import upc.com.pe.backendplannia.iam.domain.model.queries.GetTeamByIdQuery;
import upc.com.pe.backendplannia.iam.domain.services.TeamCommandService;
import upc.com.pe.backendplannia.iam.domain.services.TeamQueryService;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.CreateTeamResource;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.TeamResource;
import upc.com.pe.backendplannia.iam.interfaces.rest.transform.CreateTeamCommandFromResourceAssembler;
import upc.com.pe.backendplannia.iam.interfaces.rest.transform.TeamResourceFromEntityAssembler;
import upc.com.pe.backendplannia.shared.interfaces.rest.resources.MessageResource;

@RestController
@RequestMapping(value = "/api/v1/teams", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Teams", description = "Available Team Endpoints")
public class TeamController {
    private final TeamCommandService teamCommandService;
    private final TeamQueryService teamQueryService;

    public TeamController(TeamCommandService teamCommandService, TeamQueryService teamQueryService) {
        this.teamCommandService = teamCommandService;
        this.teamQueryService = teamQueryService;
    }

    @PostMapping
    @Operation(summary = "Create team with leader")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Team created",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TeamResource.class)
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
    public ResponseEntity<TeamResource> createTeam(@RequestBody CreateTeamResource resource) {
        var command = CreateTeamCommandFromResourceAssembler.toCommandFromResource(resource);
        var team = teamCommandService.handle(command)
                .orElseThrow(() -> new IllegalArgumentException("Team could not be created"));
        var teamResource = TeamResourceFromEntityAssembler.toResourceFromEntity(team);
        return ResponseEntity.status(HttpStatus.CREATED).body(teamResource);
    }

    @GetMapping("/{teamId}")
    @Operation(summary = "Get team by id")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Team found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TeamResource.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Team not found"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResource.class)
                    )
            )
    })
    public ResponseEntity<TeamResource> getTeamById(@PathVariable Long teamId) {
        var team = teamQueryService.handle(new GetTeamByIdQuery(teamId));
        if (team.isEmpty()) return ResponseEntity.notFound().build();
        var teamResource = TeamResourceFromEntityAssembler.toResourceFromEntity(team.get());
        return ResponseEntity.ok(teamResource);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResource> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new MessageResource(exception.getMessage()));
    }
}
