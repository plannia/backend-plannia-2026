package upc.com.pe.backendplannia.profile.domain.model.commands;

// Crea un perfil BASE al registrarse el miembro: sin skills ni embeddings (no se puede embeber texto
// vacío). El miembro lo completa luego con UpdateMemberProfileCommand, que sí genera los embeddings.
public record CreateDefaultMemberProfileCommand(Long userId, Long teamId) {
    public CreateDefaultMemberProfileCommand {
        if (userId == null) throw new IllegalArgumentException("userId cannot be null");
        if (teamId == null) throw new IllegalArgumentException("teamId cannot be null");
    }
}
