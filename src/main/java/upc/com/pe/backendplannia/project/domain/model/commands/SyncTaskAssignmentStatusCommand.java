package upc.com.pe.backendplannia.project.domain.model.commands;

import java.util.List;

public record SyncTaskAssignmentStatusCommand(List<Long> taskIds) {
    public SyncTaskAssignmentStatusCommand {
        if (taskIds == null) {
            throw new IllegalArgumentException("taskIds cannot be null");
        }
    }
}
