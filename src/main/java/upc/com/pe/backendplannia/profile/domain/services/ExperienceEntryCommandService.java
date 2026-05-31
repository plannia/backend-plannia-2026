package upc.com.pe.backendplannia.profile.domain.services;

import upc.com.pe.backendplannia.profile.domain.model.commands.AddExperienceEntryCommand;
import upc.com.pe.backendplannia.profile.domain.model.entities.ExperienceEntry;

import java.util.Optional;

public interface ExperienceEntryCommandService {
    Optional<ExperienceEntry> handle(AddExperienceEntryCommand command);
}
