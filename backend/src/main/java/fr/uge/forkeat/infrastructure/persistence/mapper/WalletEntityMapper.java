package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.service.model.user.Wallet;

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
}