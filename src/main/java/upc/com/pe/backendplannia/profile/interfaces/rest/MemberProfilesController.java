package upc.com.pe.backendplannia.profile.interfaces.rest;

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
import org.springframework.web.bind.annotation.*;
import upc.com.pe.backendplannia.profile.domain.model.commands.ReduceActiveHoursCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.UpdateActiveHoursCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.UpdateMaxHoursCommand;
import upc.com.pe.backendplannia.profile.domain.model.queries.GetAllMemberProfilesQuery;
import upc.com.pe.backendplannia.profile.domain.model.queries.GetMemberProfileByUserIdQuery;
import upc.com.pe.backendplannia.profile.domain.model.queries.GetMemberProfilesByTeamIdQuery;
import upc.com.pe.backendplannia.profile.domain.services.MemberProfileCommandService;
import upc.com.pe.backendplannia.profile.domain.services.MemberProfileQueryService;
import upc.com.pe.backendplannia.profile.interfaces.rest.resources.CreateMemberProfileResource;
import upc.com.pe.backendplannia.profile.interfaces.rest.resources.MemberProfileResource;
import upc.com.pe.backendplannia.profile.interfaces.rest.resources.ReduceActiveHoursResource;
import upc.com.pe.backendplannia.profile.interfaces.rest.resources.UpdateActiveHoursResource;
import upc.com.pe.backendplannia.profile.interfaces.rest.resources.UpdateMaxHoursResource;
import upc.com.pe.backendplannia.profile.interfaces.rest.resources.UpdateMemberProfileResource;
import upc.com.pe.backendplannia.profile.interfaces.rest.transform.CreateMemberProfileCommandFromResourceAssembler;
import upc.com.pe.backendplannia.profile.interfaces.rest.transform.MemberProfileResourceFromEntityAssembler;
import upc.com.pe.backendplannia.profile.interfaces.rest.transform.UpdateMemberProfileCommandFromResourceAssembler;
import upc.com.pe.backendplannia.shared.interfaces.rest.resources.MessageResource;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/member-profiles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Member Profiles", description = "Available Member Profile Endpoints")
public class MemberProfilesController {
    private final MemberProfileCommandService memberProfileCommandService;
    private final MemberProfileQueryService memberProfileQueryService;

    public MemberProfilesController(
            MemberProfileCommandService memberProfileCommandService,
            MemberProfileQueryService memberProfileQueryService
    ) {
        this.memberProfileCommandService = memberProfileCommandService;
        this.memberProfileQueryService = memberProfileQueryService;
    }

    @PostMapping
    @Operation(summary = "Create member profile")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Member profile created",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MemberProfileResource.class)
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
    public ResponseEntity<?> createMemberProfile(@RequestBody CreateMemberProfileResource resource) {
        try {
            var command = CreateMemberProfileCommandFromResourceAssembler.toCommandFromResource(resource);
            var memberProfile = memberProfileCommandService.handle(command);
            if (memberProfile.isEmpty()) return ResponseEntity.badRequest().build();
            var memberProfileResource = MemberProfileResourceFromEntityAssembler.toResourceFromEntity(memberProfile.get());
            return new ResponseEntity<>(memberProfileResource, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResource(e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all member profiles")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Member profiles found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = MemberProfileResource.class))
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Member profiles not found")
    })
    public ResponseEntity<List<MemberProfileResource>> getAllMemberProfiles() {
        var memberProfiles = memberProfileQueryService.handle(new GetAllMemberProfilesQuery());
        if (memberProfiles.isEmpty()) return ResponseEntity.notFound().build();
        var resources = memberProfiles.stream()
                .map(MemberProfileResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get member profile by user id")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Member profile found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MemberProfileResource.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Member profile not found")
    })
    public ResponseEntity<MemberProfileResource> getMemberProfileByUserId(@PathVariable Long userId) {
        var result = memberProfileQueryService.handle(new GetMemberProfileByUserIdQuery(userId));
        if (result.isEmpty()) return ResponseEntity.notFound().build();
        var resource = MemberProfileResourceFromEntityAssembler.toResourceFromEntity(result.get());
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/teams/{teamId}")
    @Operation(summary = "Get member profiles by team id")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Member profiles found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = MemberProfileResource.class))
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Member profiles not found")
    })
    public ResponseEntity<List<MemberProfileResource>> getMemberProfilesByTeamId(@PathVariable Long teamId) {
        var memberProfiles = memberProfileQueryService.handle(new GetMemberProfilesByTeamIdQuery(teamId));
        if (memberProfiles.isEmpty()) return ResponseEntity.notFound().build();
        var resources = memberProfiles.stream()
                .map(MemberProfileResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @PutMapping("/users/{userId}")
    @Operation(summary = "Update member profile")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Member profile updated",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MemberProfileResource.class)
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
    public ResponseEntity<?> updateMemberProfile(
            @PathVariable Long userId,
            @RequestBody UpdateMemberProfileResource resource
    ) {
        try {
            var command = UpdateMemberProfileCommandFromResourceAssembler.toCommandFromResource(userId, resource);
            var updatedMemberProfile = memberProfileCommandService.handle(command);
            if (updatedMemberProfile.isEmpty()) return ResponseEntity.notFound().build();
            var updatedResource = MemberProfileResourceFromEntityAssembler.toResourceFromEntity(updatedMemberProfile.get());
            return ResponseEntity.ok(updatedResource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResource(e.getMessage()));
        }
    }

    @PatchMapping("/users/{userId}/max-hours")
    @Operation(summary = "Update member profile max hours")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Member profile max hours updated",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MemberProfileResource.class)
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
    public ResponseEntity<?> updateMaxHours(
            @PathVariable Long userId,
            @RequestBody UpdateMaxHoursResource resource
    ) {
        try {
            var updatedMemberProfile = memberProfileCommandService.handle(
                    new UpdateMaxHoursCommand(userId, resource.maxHours())
            );
            if (updatedMemberProfile.isEmpty()) return ResponseEntity.notFound().build();
            var updatedResource = MemberProfileResourceFromEntityAssembler.toResourceFromEntity(updatedMemberProfile.get());
            return ResponseEntity.ok(updatedResource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResource(e.getMessage()));
        }
    }

    /* Debe ser llamado por el sistema automaticamente cuando se asigna una tarea
    @PatchMapping("/users/{userId}/active-hours")
    @Operation(summary = "Increase member profile active hours")
    public ResponseEntity<?> updateActiveHours(
            @PathVariable Long userId,
            @RequestBody UpdateActiveHoursResource resource
    ) {
        try {
            var updatedMemberProfile = memberProfileCommandService.handle(
                    new UpdateActiveHoursCommand(userId, resource.hours())
            );
            if (updatedMemberProfile.isEmpty()) return ResponseEntity.notFound().build();
            var updatedResource = MemberProfileResourceFromEntityAssembler.toResourceFromEntity(updatedMemberProfile.get());
            return ResponseEntity.ok(updatedResource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResource(e.getMessage()));
        }
    }*/

    @PatchMapping("/users/{userId}/active-hours/reduce")
    @Operation(summary = "Reduce member profile active hours")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Member profile active hours reduced",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MemberProfileResource.class)
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
    public ResponseEntity<?> reduceActiveHours(
            @PathVariable Long userId,
            @RequestBody ReduceActiveHoursResource resource
    ) {
        try {
            var updatedMemberProfile = memberProfileCommandService.handle(
                    new ReduceActiveHoursCommand(userId, resource.hours())
            );
            if (updatedMemberProfile.isEmpty()) return ResponseEntity.notFound().build();
            var updatedResource = MemberProfileResourceFromEntityAssembler.toResourceFromEntity(updatedMemberProfile.get());
            return ResponseEntity.ok(updatedResource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResource(e.getMessage()));
        }
    }
}
