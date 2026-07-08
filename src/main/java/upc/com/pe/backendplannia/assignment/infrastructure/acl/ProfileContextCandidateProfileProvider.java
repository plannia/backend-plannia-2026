package upc.com.pe.backendplannia.assignment.infrastructure.acl;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;
import upc.com.pe.backendplannia.assignment.domain.services.CandidateProfileProvider;
import upc.com.pe.backendplannia.profile.domain.model.aggregates.MemberProfile;
import upc.com.pe.backendplannia.profile.domain.model.queries.GetMemberProfileByUserIdQuery;
import upc.com.pe.backendplannia.profile.domain.model.queries.GetMemberProfilesByTeamIdQuery;
import upc.com.pe.backendplannia.profile.domain.services.MemberExperienceQueryService;
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
    private final MemberExperienceQueryService memberExperienceQueryService;

    public ProfileContextCandidateProfileProvider(
            MemberProfileQueryService memberProfileQueryService,
            MemberExperienceQueryService memberExperienceQueryService
    ) {
        this.memberProfileQueryService = memberProfileQueryService;
        this.memberExperienceQueryService = memberExperienceQueryService;
    }

    @Override
    public Optional<CandidateProfile> findByUserId(Long userId) {
        return memberProfileQueryService.handle(new GetMemberProfileByUserIdQuery(userId))
                .map(this::toCandidateProfile);
    }

    @Override
    public List<CandidateProfile> findByTeamId(Long teamId) {
        return memberProfileQueryService.handle(new GetMemberProfilesByTeamIdQuery(teamId)).stream()
                // Excluimos perfiles BASE/incompletos (sin embedding de habilidades): un miembro recién
                // registrado que aún no cargó sus skills no es un candidato real hasta completar su perfil.
                .filter(this::hasSkills)
                .map(this::toCandidateProfile)
                .toList();
    }

    private boolean hasSkills(MemberProfile memberProfile) {
        return memberProfile.getEmbeddedAbilities() != null
                && memberProfile.getEmbeddedAbilities().dimension() > 0;
    }

    private CandidateProfile toCandidateProfile(MemberProfile memberProfile) {
        return new CandidateProfile(
                memberProfile.getUserId(),
                memberProfile.getEmbeddedAbilities(),
                memberProfile.getEmbeddedExperience(),
                memberProfile.getEmbeddedInterests(),
                memberExperienceQueryService.findExperienceEmbeddings(memberProfile.getUserId()),
                memberProfile.getActiveHours(),
                memberProfile.getMaxHours()
        );
    }
}
