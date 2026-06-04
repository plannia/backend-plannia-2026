package upc.com.pe.backendplannia.profile.domain.services;

import upc.com.pe.backendplannia.profile.domain.model.aggregates.MemberProfile;
import upc.com.pe.backendplannia.profile.domain.model.commands.CreateDefaultMemberProfileCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.CreateMemberProfileCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.ReduceActiveHoursCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.UpdateActiveHoursCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.UpdateMaxHoursCommand;
import upc.com.pe.backendplannia.profile.domain.model.commands.UpdateMemberProfileCommand;

import java.util.Optional;

public interface MemberProfileCommandService {
    Optional<MemberProfile> handle(CreateMemberProfileCommand command);

    Optional<MemberProfile> handle(CreateDefaultMemberProfileCommand command);

    Optional<MemberProfile> handle(UpdateMemberProfileCommand command);

    Optional<MemberProfile> handle(UpdateMaxHoursCommand command);

    Optional<MemberProfile> handle(UpdateActiveHoursCommand command);

    Optional<MemberProfile> handle(ReduceActiveHoursCommand command);
}
