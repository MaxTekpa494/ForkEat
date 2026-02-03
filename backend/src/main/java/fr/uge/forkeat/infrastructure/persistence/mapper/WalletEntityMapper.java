package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.service.model.user.Wallet;
import org.springframework.stereotype.Component;

import java.util.Objects;

public class WalletEntityMapper {

    private WalletEntityMapper() {}

    public static Wallet toDomain(WalletEntity entity) {
        Objects.requireNonNull(entity, "WalletEntity cannot be null");
        return new Wallet(
                entity.getId(),
                entity.getUser().getId(),
                entity.getBalance(),
                entity.getUpdatedAt()
        );
    }

    // COMMENTAIRE DE MAX : ÇA SERT À QUOI ÇA ????
    public static void updateEntity(WalletEntity entity, Wallet domain) {
        Objects.requireNonNull(entity, "WalletEntity cannot be null");
        Objects.requireNonNull(domain, "Wallet cannot be null");

        entity.setBalance(domain.balance());
        entity.setUpdatedAt(domain.updatedAt());
        // On ne touche pas à l'ID ni au User ici (à gérer par l'adapter si besoin)
    }
}