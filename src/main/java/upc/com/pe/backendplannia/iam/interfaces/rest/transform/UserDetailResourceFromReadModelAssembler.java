package upc.com.pe.backendplannia.iam.interfaces.rest.transform;

import upc.com.pe.backendplannia.iam.domain.model.readmodels.MemberProfileSnapshot;
import upc.com.pe.backendplannia.iam.domain.model.readmodels.UserDetailReadModel;
import upc.com.pe.backendplannia.iam.domain.model.readmodels.UserTaskStatusCounts;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.UserDetailProfileResource;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.UserDetailResource;
import upc.com.pe.backendplannia.iam.interfaces.rest.resources.UserTaskStatusCountsResource;

public class UserDetailResourceFromReadModelAssembler {
    public static UserDetailResource toResourceFromReadModel(UserDetailReadModel readModel) {
        return new UserDetailResource(
                readModel.id(),
                readModel.name(),
                readModel.email(),
                readModel.position(),
                readModel.role(),
                readModel.teamId(),
                toProfileResource(readModel.profile()),
                toTaskStatusCountsResource(readModel.taskStatusCounts())
        );
    }

    private static UserDetailProfileResource toProfileResource(MemberProfileSnapshot profile) {
        if (profile == null) {
            return null;
        }
        return new UserDetailProfileResource(
                profile.id(),
                profile.userId(),
                profile.teamId(),
                profile.maxHours(),
                profile.abilities(),
                profile.interests(),
                profile.activeHours()
        );
    }

    private static UserTaskStatusCountsResource toTaskStatusCountsResource(UserTaskStatusCounts counts) {
        return new UserTaskStatusCountsResource(
                counts.toDoCount(),
                counts.inProgressCount(),
                counts.doneCount()
        );
    }
}
