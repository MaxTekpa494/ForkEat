package fr.uge.forkeat.infrastructure.persistence.postgres.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.service.model.Wallet;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class WalletMapper {

    public Wallet toDomain(WalletEntity entity) {
        Objects.requireNonNull(entity, "WalletEntity cannot be null");

        return new Wallet(
                entity.getId(),
                entity.getBalance(),
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getUpdatedAt()
        );
    }
    public void updateEntity(WalletEntity entity, Wallet domain) {
        Objects.requireNonNull(entity, "WalletEntity cannot be null");
        Objects.requireNonNull(domain, "Wallet cannot be null");

        entity.setBalance(domain.balance());
        entity.setUpdatedAt(domain.updatedAt());
        // On ne touche pas à l'ID ni au User ici (à gérer par l'adapter si besoin)
    }
}