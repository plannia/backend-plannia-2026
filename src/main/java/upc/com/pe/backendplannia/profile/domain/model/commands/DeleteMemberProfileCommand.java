package upc.com.pe.backendplannia.profile.domain.model.commands;

// Borra el perfil del miembro (y sus entradas de experiencia). Se dispara al eliminarse el usuario en IAM.
public record DeleteMemberProfileCommand(Long userId) {
    public DeleteMemberProfileCommand {
        if (userId == null) throw new IllegalArgumentException("userId cannot be null");
    }
}
