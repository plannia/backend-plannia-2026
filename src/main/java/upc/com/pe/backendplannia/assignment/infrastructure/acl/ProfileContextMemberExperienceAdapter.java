package upc.com.pe.backendplannia.assignment.infrastructure.acl;

import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.assignment.domain.services.MemberExperiencePort;
import upc.com.pe.backendplannia.profile.domain.model.commands.AddExperienceEntryCommand;
import upc.com.pe.backendplannia.profile.domain.services.ExperienceEntryCommandService;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

/**
 * Adaptador ACL: traduce "registrar experiencia" al comando de Profile, que además recalcula el
 * promedio del embedding de experiencia del miembro.
 */
@Service
public class ProfileContextMemberExperienceAdapter implements MemberExperiencePort {
    private final ExperienceEntryCommandService experienceEntryCommandService;

    public ProfileContextMemberExperienceAdapter(ExperienceEntryCommandService experienceEntryCommandService) {
        this.experienceEntryCommandService = experienceEntryCommandService;
    }

    @Override
    public void recordExperience(Long userId, Long taskId, EmbeddingVector taskEmbedding) {
        experienceEntryCommandService.handle(new AddExperienceEntryCommand(userId, taskId, taskEmbedding));
    }
}
