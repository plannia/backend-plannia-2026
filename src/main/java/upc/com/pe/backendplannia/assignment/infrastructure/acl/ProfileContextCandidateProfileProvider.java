package upc.com.pe.backendplannia.assignment.infrastructure.acl;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;
import upc.com.pe.backendplannia.assignment.domain.services.CandidateProfileProvider;
import upc.com.pe.backendplannia.profile.domain.model.aggregates.MemberProfile;
import upc.com.pe.backendplannia.profile.domain.model.queries.GetMemberProfileByUserIdQuery;
import upc.com.pe.backendplannia.profile.domain.model.queries.GetMemberProfilesByTeamIdQuery;
import upc.com.pe.backendplannia.profile.domain.services.MemberProfileQueryService;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador ACL: traduce los aggregates de Profile al read model {@link CandidateProfile} de Assignment.
 * Delega en la API pública de Profile ({@link MemberProfileQueryService}), no en sus repositorios.
 */
@Service
public class ProfileContextCandidateProfileProvider implements CandidateProfileProvider {
    private final MemberProfileQueryService memberProfileQueryService;

    public ProfileContextCandidateProfileProvider(MemberProfileQueryService memberProfileQueryService) {
        this.memberProfileQueryService = memberProfileQueryService;
    }

    @Override
    public Optional<CandidateProfile> findByUserId(Long userId) {
        return memberProfileQueryService.handle(new GetMemberProfileByUserIdQuery(userId))
                .map(this::toCandidateProfile);
    }

    @Override
    public List<CandidateProfile> findByTeamId(Long teamId) {
        return memberProfileQueryService.handle(new GetMemberProfilesByTeamIdQuery(teamId)).stream()
                .map(this::toCandidateProfile)
                .toList();
    }

    private CandidateProfile toCandidateProfile(MemberProfile memberProfile) {
        return new CandidateProfile(
                memberProfile.getUserId(),
                memberProfile.getEmbeddedAbilities(),
                memberProfile.getEmbeddedExperience(),
                memberProfile.getEmbeddedInterests(),
                memberProfile.getActiveHours(),
                memberProfile.getMaxHours()
        );
    }
}
