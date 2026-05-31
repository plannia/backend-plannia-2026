package upc.com.pe.backendplannia.assignment.domain.services;

import upc.com.pe.backendplannia.assignment.domain.model.readmodels.CandidateProfile;

import java.util.List;
import java.util.Optional;

/**
 * Puerto (ACL) para leer candidatos desde el contexto Profile en el lenguaje de Assignment.
 * Devuelve el read model propio {@link CandidateProfile}, nunca el aggregate de Profile.
 */
public interface CandidateProfileProvider {
    Optional<CandidateProfile> findByUserId(Long userId);

    List<CandidateProfile> findByTeamId(Long teamId);
}
