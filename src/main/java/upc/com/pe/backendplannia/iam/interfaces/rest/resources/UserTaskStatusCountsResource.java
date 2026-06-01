package upc.com.pe.backendplannia.iam.interfaces.rest.resources;

public record UserTaskStatusCountsResource(
        long toDoCount,
        long inProgressCount,
        long doneCount
) {
}
