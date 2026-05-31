package upc.com.pe.backendplannia.iam.interfaces.rest.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import upc.com.pe.backendplannia.iam.domain.model.commands.DeleteUserCommand;
import upc.com.pe.backendplannia.iam.domain.services.UserCommandService;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.UpdateUserResource;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.UserResource;
import upc.com.pe.backendplannia.iam.interfaces.rest.transform.UpdateUserCommandFromResourceAssembler;
import upc.com.pe.backendplannia.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import upc.com.pe.backendplannia.shared.interfaces.rest.resources.MessageResource;

@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "Available User Endpoints")
public class UserController {
    private final UserCommandService userCommandService;

    public UserController(UserCommandService userCommandService) {
        this.userCommandService = userCommandService;
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user by id")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User updated",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResource.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResource.class)
                    )
            )
    })
    public ResponseEntity<UserResource> updateUser(
            @PathVariable Long userId,
            @RequestBody UpdateUserResource resource
    ) {
        var command = UpdateUserCommandFromResourceAssembler.toCommandFromResource(userId, resource);
        var updatedUser = userCommandService.handle(command);
        if (updatedUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(updatedUser.get()));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        var deleted = userCommandService.handle(new DeleteUserCommand(userId));
        if (deleted.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResource> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new MessageResource(exception.getMessage()));
    }
}
