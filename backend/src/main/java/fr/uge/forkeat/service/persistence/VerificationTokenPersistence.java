package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.user.VerificationToken;
import fr.uge.forkeat.service.model.user.VerificationTokenType;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenPersistence {
    VerificationToken save(VerificationToken token);
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUserIdAndType(UUID userId, VerificationTokenType type);
    void deleteByUserIdAndType(UUID userId, VerificationTokenType type);
}
