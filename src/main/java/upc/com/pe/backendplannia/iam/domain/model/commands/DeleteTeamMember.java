package upc.com.pe.backendplannia.iam.domain.model.commands;

public record DeleteTeamMember(Long teamId, Long userId) {
    public DeleteTeamMember {
        if (userId == null || teamId == null) throw new IllegalArgumentException("userId or teamId cannot be null");
    }
}
