package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.BankInfoEntity;
import fr.uge.forkeat.service.model.user.BankInfo;

import java.util.Objects;

public class BankInfoEntityMapper {


    public static BankInfo toDomain(BankInfoEntity entity) {
        Objects.requireNonNull(entity);
        Objects.requireNonNull(entity.getUser()); // User must be present for BankInfo
        return new BankInfo(entity.getUser().getId(), entity.getBankName(), entity.getExternalAccountId());
    }
}
