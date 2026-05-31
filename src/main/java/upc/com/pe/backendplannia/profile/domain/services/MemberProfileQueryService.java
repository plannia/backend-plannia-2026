package upc.com.pe.backendplannia.profile.domain.services;

import upc.com.pe.backendplannia.profile.domain.model.aggregates.MemberProfile;
import upc.com.pe.backendplannia.profile.domain.model.queries.GetAllMemberProfilesQuery;
import upc.com.pe.backendplannia.profile.domain.model.queries.GetMemberProfileByUserIdQuery;
import upc.com.pe.backendplannia.profile.domain.model.queries.GetMemberProfilesByTeamIdQuery;

import java.util.List;
import java.util.Optional;

public interface MemberProfileQueryService {
    List<MemberProfile> handle(GetAllMemberProfilesQuery query);

    Optional<MemberProfile> handle(GetMemberProfileByUserIdQuery query);

    List<MemberProfile> handle(GetMemberProfilesByTeamIdQuery query);
}
