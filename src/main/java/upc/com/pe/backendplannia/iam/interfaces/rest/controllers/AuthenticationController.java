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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import upc.com.pe.backendplannia.iam.domain.services.UserCommandService;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.AuthenticatedUserResource;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.SignInResource;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.SignUpResource;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.UserResource;
import upc.com.pe.backendplannia.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import upc.com.pe.backendplannia.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;
import upc.com.pe.backendplannia.iam.interfaces.rest.transform.SignUpCommandFromResourceAssembler;
import upc.com.pe.backendplannia.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import upc.com.pe.backendplannia.shared.interfaces.rest.resources.MessageResource;

@RestController
@RequestMapping(value = "/api/v1/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Available Authentication Endpoints")
public class AuthenticationController {
    private final UserCommandService userCommandService;

    public AuthenticationController(UserCommandService userCommandService) {
        this.userCommandService = userCommandService;
    }

    @PostMapping("/sign-up")
    @Operation(summary = "Sign up a new team member")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResource.class)
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
    public ResponseEntity<UserResource> signUp(@RequestBody SignUpResource resource) {
        var command = SignUpCommandFromResourceAssembler.toCommandFromResource(resource);
        var user = userCommandService.handle(command)
                .orElseThrow(() -> new IllegalArgumentException("User could not be registered"));
        var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResource);
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Sign in with email and password")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User authenticated",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthenticatedUserResource.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResource.class)
                    )
            )
    })
    public ResponseEntity<AuthenticatedUserResource> signIn(@RequestBody SignInResource resource) {
        var command = SignInCommandFromResourceAssembler.toCommandFromResource(resource);
        var authenticatedUser = userCommandService.handle(command);
        if (authenticatedUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var authenticatedUserResource = AuthenticatedUserResourceFromEntityAssembler.toResourceFromEntityAndToken(
                authenticatedUser.get().getLeft(),
                authenticatedUser.get().getRight()
        );
        return ResponseEntity.ok(authenticatedUserResource);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResource> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new MessageResource(exception.getMessage()));
    }
}
