package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.BankInfoEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.service.model.user.BankInfo;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.Wallet;

public final class UserEntityMapper {

    private UserEntityMapper() {}

    public  User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getRole(),
                entity.getStatus(),
                entity.getAuthMode(),
                toBankInfo(entity.getBankInfo()),
                toWallet(entity.getWallet()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public  Wallet toWallet(WalletEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Wallet(
                entity.getId(),
                entity.getBalance(),
                entity.getUpdatedAt()
        );
    }

    public static BankInfo toBankInfo(BankInfoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new BankInfo(
                entity.getBankName(),
                entity.getIban(),
                entity.getBic()
        );
    }
}
