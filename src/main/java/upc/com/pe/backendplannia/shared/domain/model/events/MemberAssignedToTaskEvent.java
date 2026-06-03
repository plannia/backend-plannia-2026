package upc.com.pe.backendplannia.shared.domain.model.events;

// Cruza de contexto: lo publica Assignment al crear/confirmar una asignación y lo consume Notifications.
// Solo viajan los identificadores; Notifications resuelve el contacto del usuario vía ACL hacia IAM.
public record MemberAssignedToTaskEvent(Long userId, Long taskId) {
    public MemberAssignedToTaskEvent {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (taskId == null) {
            throw new IllegalArgumentException("taskId cannot be null");
        }
    }
}
