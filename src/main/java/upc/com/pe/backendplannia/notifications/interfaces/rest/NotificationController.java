package upc.com.pe.backendplannia.notifications.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import upc.com.pe.backendplannia.notifications.domain.model.queries.GetAllNotificationsQuery;
import upc.com.pe.backendplannia.notifications.domain.model.queries.GetNotificationsByUserIdQuery;
import upc.com.pe.backendplannia.notifications.domain.services.NotificationQueryService;
import upc.com.pe.backendplannia.notifications.interfaces.rest.resources.NotificationResource;
import upc.com.pe.backendplannia.notifications.interfaces.rest.transform.NotificationResourceFromEntityAssembler;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Available Notification Endpoints")
public class NotificationController {
    private final NotificationQueryService notificationQueryService;

    public NotificationController(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @GetMapping
    @Operation(summary = "Get all sent notifications (most recent first)")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Notifications found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = NotificationResource.class))
                    )
            )
    })
    public ResponseEntity<List<NotificationResource>> getAllNotifications() {
        var notifications = notificationQueryService.handle(new GetAllNotificationsQuery());
        var resources = notifications.stream()
                .map(NotificationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get notifications by user id")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Notifications found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = NotificationResource.class))
                    )
            )
    })
    public ResponseEntity<List<NotificationResource>> getNotificationsByUserId(@PathVariable Long userId) {
        var notifications = notificationQueryService.handle(new GetNotificationsByUserIdQuery(userId));
        var resources = notifications.stream()
                .map(NotificationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }
}
