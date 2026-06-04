package upc.com.pe.backendplannia.shared.domain.model.events;

// Cruza de contexto: lo publica IAM al registrarse un miembro (SignUp) y lo consume Profile para
// crearle automáticamente un perfil base (sin skills aún). Así nunca queda un miembro sin perfil.
public record MemberRegisteredEvent(Long userId, Long teamId) {
    public MemberRegisteredEvent {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (teamId == null) {
            throw new IllegalArgumentException("teamId cannot be null");
        }
    }
}
