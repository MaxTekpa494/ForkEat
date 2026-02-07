package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.BankInfoEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.service.model.user.BankInfo;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.Wallet;

import java.util.Objects;

public final class UserEntityMapper {

    private UserEntityMapper() {}

    public  static User toDomain(UserEntity entity) {
        Objects.requireNonNull(entity);
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getRole(),
                entity.getStatus(),
                entity.getAuthMode(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static UserEntity toEntity(User user) {
        Objects.requireNonNull(user);
        var entity = new UserEntity();
        entity.setId(user.id());
        entity.setUsername(user.username());
        entity.setFirstName(user.firstName());
        entity.setLastName(user.lastName());
        entity.setEmail(user.email());
        entity.setRole(user.role());
        entity.setStatus(user.status());
        entity.setAuthMode(user.authMode());
        entity.setCreatedAt(user.createdAt());
        entity.setUpdatedAt(user.updatedAt());
        return entity;
    }

    private static BankInfoEntity toBankInfoEntity(BankInfo bankInfo, UserEntity user) {
        if (bankInfo == null) {
            return null;
        }
        return new BankInfoEntity(
                bankInfo.bankName(),
                bankInfo.iban(),
                bankInfo.bic(),
                user
        );
    }

//    private static WalletEntity toWalletEntity(Wallet wallet, UserEntity user) {
//        if (wallet == null) {
//            return null;
//        }
//        var entity = new WalletEntity(wallet.balance(), user);
//        entity.setId(wallet.id());
//        entity.setUpdatedAt(wallet.updatedAt());
//        return entity;
//    }
//
//    private static Wallet toWallet(WalletEntity entity) {
//        if (entity == null) {
//            return null;
//        }
//        return new Wallet(
//                entity.getId(),
//                entity.getBalance(),
//                entity.getUpdatedAt()
//        );
//    }
//
//    private static BankInfo toBankInfo(BankInfoEntity entity) {
//        if (entity == null) {
//            return null;
//        }
//        return new BankInfo(
//                entity.getBankName(),
//                entity.getIban(),
//                entity.getBic()
//        );
//    }
}
