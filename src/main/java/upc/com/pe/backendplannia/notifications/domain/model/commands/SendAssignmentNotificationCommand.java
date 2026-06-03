package upc.com.pe.backendplannia.notifications.domain.model.commands;

public record SendAssignmentNotificationCommand(Long userId, Long taskId) {
    public SendAssignmentNotificationCommand {
        if (userId == null) throw new IllegalArgumentException("userId cannot be null");
        if (taskId == null) throw new IllegalArgumentException("taskId cannot be null");
    }
}
