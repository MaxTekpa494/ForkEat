package fr.uge.forkeat.service.model.user;

import java.util.Objects;
import java.util.UUID;

public record BankInfo(UUID userId, String bankName, String externalAccountId) {

    public BankInfo{
        Objects.requireNonNull(userId);
        Objects.requireNonNull(bankName);
        Objects.requireNonNull(externalAccountId);
    }
}