package upc.com.pe.backendplannia.assignment.application.internal.outboundservices;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.TaskRequirement;
import upc.com.pe.backendplannia.assignment.domain.services.TaskRequirementResolver;

/**
 * Centraliza la resolución del requisito de tarea (antes duplicada en command y query service).
 * Usa {@link ObjectProvider} porque el contexto Project aún no existe: el resolver puede no estar
 * presente en tiempo de ejecución y aquí se traduce esa ausencia a un error claro.
 */
@Component
public class TaskRequirementGateway {
    private final ObjectProvider<TaskRequirementResolver> taskRequirementResolverProvider;

    public TaskRequirementGateway(ObjectProvider<TaskRequirementResolver> taskRequirementResolverProvider) {
        this.taskRequirementResolverProvider = taskRequirementResolverProvider;
    }

    public TaskRequirement requireByTaskId(Long taskId) {
        var resolver = taskRequirementResolverProvider.getIfAvailable();
        if (resolver == null) {
            throw new IllegalStateException("Task requirement resolver is not configured");
        }
        return resolver.resolveByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task requirement not found for task id: " + taskId));
    }
}
