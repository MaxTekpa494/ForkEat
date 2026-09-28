package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.PlatformWalletEntity;
import fr.uge.forkeat.service.model.wallet.PlatformWallet;

import java.util.Objects;

public class PlatformWalletEntityMapper {

    private PlatformWalletEntityMapper() {}

    public static PlatformWallet toDomain(PlatformWalletEntity entity) {
        Objects.requireNonNull(entity, "PlatformWalletEntity cannot be null");
        Objects.requireNonNull(entity.getWallet(), "Associated wallet cannot be null");
        return new PlatformWallet(
                entity.getId(),
                entity.getType(),
                entity.getWallet().getBalance(),
                entity.getWallet().getUpdatedAt()
        );
    }
}
