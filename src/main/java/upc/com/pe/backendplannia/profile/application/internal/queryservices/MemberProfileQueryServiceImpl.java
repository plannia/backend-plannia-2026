package upc.com.pe.backendplannia.profile.application.internal.queryservices;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.profile.domain.model.aggregates.MemberProfile;
import upc.com.pe.backendplannia.profile.domain.model.queries.GetAllMemberProfilesQuery;
import upc.com.pe.backendplannia.profile.domain.model.queries.GetMemberProfileByUserIdQuery;
import upc.com.pe.backendplannia.profile.domain.model.queries.GetMemberProfilesByTeamIdQuery;
import upc.com.pe.backendplannia.profile.domain.services.MemberProfileQueryService;
import upc.com.pe.backendplannia.profile.infrastructure.persistence.jpa.repositories.MemberProfileRepository;

import java.util.List;
import java.util.Optional;

@Service
public class MemberProfileQueryServiceImpl implements MemberProfileQueryService {
    private final MemberProfileRepository memberProfileRepository;

    public MemberProfileQueryServiceImpl(MemberProfileRepository memberProfileRepository) {
        this.memberProfileRepository = memberProfileRepository;
    }

    @Override
    public List<MemberProfile> handle(GetAllMemberProfilesQuery query) {
        return memberProfileRepository.findAll();
    }

    @Override
    public Optional<MemberProfile> handle(GetMemberProfileByUserIdQuery query) {
        return memberProfileRepository.findByUserId(query.userId());
    }

    @Override
    public List<MemberProfile> handle(GetMemberProfilesByTeamIdQuery query) {
        return memberProfileRepository.findByTeamId(query.teamId());
    }
}
