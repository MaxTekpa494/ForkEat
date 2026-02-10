package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.VerificationTokenEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.VerificationTokenRepository;
import fr.uge.forkeat.service.model.user.VerificationToken;
import fr.uge.forkeat.service.model.user.VerificationTokenType;
import fr.uge.forkeat.service.persistence.VerificationTokenPersistence;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class VerificationTokenPersistenceAdapter implements VerificationTokenPersistence {

    private final VerificationTokenRepository repository;

    public VerificationTokenPersistenceAdapter(VerificationTokenRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public VerificationToken save(VerificationToken token) {
        Objects.requireNonNull(token);
        var entity = VerificationTokenEntityMapper.toEntity(token);
        var saved = repository.save(entity);
        return VerificationTokenEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<VerificationToken> findByToken(String token) {
        Objects.requireNonNull(token);
        return repository.findByToken(token).map(VerificationTokenEntityMapper::toDomain);
    }

    @Override
    public Optional<VerificationToken> findByUserIdAndType(UUID userId, VerificationTokenType type) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(userId);
        return repository.findByUserIdAndType(userId, type).map(VerificationTokenEntityMapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteByUserIdAndType(UUID userId, VerificationTokenType type) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(userId);
        repository.deleteByUserIdAndType(userId, type);
        repository.flush();
    }
}
