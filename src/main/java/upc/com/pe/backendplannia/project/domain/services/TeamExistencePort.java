package upc.com.pe.backendplannia.project.domain.services;

/**
 * Puerto ACL hacia IAM para validar equipos sin acoplar el aggregate {@code Team}.
 */
public interface TeamExistencePort {
    boolean existsById(Long teamId);
}
