package upc.com.pe.backendplannia.iam.domain.model.readmodels;

import upc.com.pe.backendplannia.iam.domain.model.valueobjects.Role;

// Vista mínima para notificaciones: solo lo necesario para contactar al usuario, sin perfil ni estadísticas.
public record UserContactReadModel(
        Long id,
        String name,
        String email,
        Role role
) {
}
