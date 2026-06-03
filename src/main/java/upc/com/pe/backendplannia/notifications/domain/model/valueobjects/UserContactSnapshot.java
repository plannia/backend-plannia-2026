package upc.com.pe.backendplannia.notifications.domain.model.valueobjects;

// Vista del usuario tal como la necesita Notifications. Es un tipo PROPIO de este contexto:
// el ACL hacia IAM traduce el Role de iam a un simple "teamMember" para no acoplar dominios.
public record UserContactSnapshot(
        Long userId,
        String name,
        String email,
        boolean teamMember
) {
}
