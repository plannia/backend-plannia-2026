package upc.com.pe.backendplannia.shared.test;

import upc.com.pe.backendplannia.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;

public final class AuditableEntityTestSupport {
    private AuditableEntityTestSupport() {
    }

    public static void assignId(AuditableAbstractAggregateRoot<?> entity, Long id) {
        try {
            var field = AuditableAbstractAggregateRoot.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign entity id for tests", exception);
        }
    }
}
