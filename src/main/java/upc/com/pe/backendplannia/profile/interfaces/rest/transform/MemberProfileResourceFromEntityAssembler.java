package upc.com.pe.backendplannia.profile.interfaces.rest.transform;

import upc.com.pe.backendplannia.profile.domain.model.aggregates.MemberProfile;
import upc.com.pe.backendplannia.profile.interfaces.rest.resources.MemberProfileResource;

public class MemberProfileResourceFromEntityAssembler {
    public static MemberProfileResource toResourceFromEntity(MemberProfile memberProfile) {
        return new MemberProfileResource(
                memberProfile.getId(),
                memberProfile.getUserId(),
                memberProfile.getTeamId(),
                memberProfile.getMaxHours(),
                memberProfile.getAbilities(),
                memberProfile.getInterests(),
                memberProfile.getActiveHours()
        );
    }
}
