package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.user.BankInfo;

import java.util.Optional;
import java.util.UUID;

public interface BankInfoPersistence {
    Optional<BankInfo> findByUserId(UUID userId);
    BankInfo saveBankInfo(BankInfo bankInfo, UUID userId);
    void deleteBankInfo(UUID bankInfoId);
}