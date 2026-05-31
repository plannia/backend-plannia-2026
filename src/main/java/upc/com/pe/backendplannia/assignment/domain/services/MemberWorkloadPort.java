package upc.com.pe.backendplannia.assignment.domain.services;

/**
 * Puerto (ACL) para reservar/liberar la carga horaria de un miembro en el contexto Profile.
 * Assignment expresa su intención (reservar al asignar, liberar al completar) sin conocer cómo
 * Profile persiste las horas.
 */
public interface MemberWorkloadPort {
    void reserveHours(Long userId, float hours);

    void releaseHours(Long userId, float hours);
}
