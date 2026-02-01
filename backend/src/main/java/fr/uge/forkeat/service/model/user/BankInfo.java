package fr.uge.forkeat.service.model.user;

import java.util.Objects;

public record BankInfo(String bankName, String iban, String bic) {

    public BankInfo{
        Objects.requireNonNull(bankName);
        Objects.requireNonNull(iban);
        Objects.requireNonNull(bic);
    }
}