package upc.com.pe.backendplannia.assignment.application.internal;

import org.junit.jupiter.api.Test;
import upc.com.pe.backendplannia.assignment.application.internal.eventhandlers.AssignmentCompletedEventHandler;
import upc.com.pe.backendplannia.assignment.domain.model.events.AssignmentCompletedEvent;
import upc.com.pe.backendplannia.assignment.domain.services.MemberExperiencePort;
import upc.com.pe.backendplannia.assignment.domain.services.MemberWorkloadPort;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AssignmentCompletedEventHandlerTests {
    private static final Long TASK_ID = 501L;
    private static final Long USER_ID = 101L;

    @Test
    void onAssignmentCompletedRecordsExperienceAndReleasesWorkload() {
        var memberExperiencePort = mock(MemberExperiencePort.class);
        var memberWorkloadPort = mock(MemberWorkloadPort.class);
        var handler = new AssignmentCompletedEventHandler(memberExperiencePort, memberWorkloadPort);
        var taskEmbedding = EmbeddingVector.of(List.of(1f, 0f, 0f));

        handler.on(new AssignmentCompletedEvent(TASK_ID, USER_ID, 4, taskEmbedding));

        verify(memberExperiencePort).recordExperience(USER_ID, TASK_ID, taskEmbedding);
        verify(memberWorkloadPort).releaseHours(USER_ID, 4);
    }
}
