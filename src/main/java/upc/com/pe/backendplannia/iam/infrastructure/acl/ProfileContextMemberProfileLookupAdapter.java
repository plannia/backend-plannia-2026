package upc.com.pe.backendplannia.iam.infrastructure.acl;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.iam.domain.model.readmodels.MemberProfileSnapshot;
import upc.com.pe.backendplannia.iam.domain.services.MemberProfileLookupPort;
import upc.com.pe.backendplannia.profile.domain.model.queries.GetMemberProfileByUserIdQuery;
import upc.com.pe.backendplannia.profile.domain.services.MemberProfileQueryService;

import java.util.Optional;

@Service
public class ProfileContextMemberProfileLookupAdapter implements MemberProfileLookupPort {
    private final MemberProfileQueryService memberProfileQueryService;

    public ProfileContextMemberProfileLookupAdapter(MemberProfileQueryService memberProfileQueryService) {
        this.memberProfileQueryService = memberProfileQueryService;
    }

    @Override
    public Optional<MemberProfileSnapshot> findByUserId(Long userId) {
        return memberProfileQueryService.handle(new GetMemberProfileByUserIdQuery(userId))
                .map(profile -> new MemberProfileSnapshot(
                        profile.getId(),
                        profile.getUserId(),
                        profile.getTeamId(),
                        profile.getMaxHours(),
                        profile.getAbilities(),
                        profile.getInterests(),
                        profile.getActiveHours()
                ));
    }
}
