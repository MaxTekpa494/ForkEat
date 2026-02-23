package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.external.PayoutGateway;
import fr.uge.forkeat.service.model.user.BankInfo;
import fr.uge.forkeat.service.persistence.BankInfoPersistence;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class BankInfoService {

    private final BankInfoPersistence bankInfoPersistence;
    private final PayoutGateway payoutGateway;

    public BankInfoService(BankInfoPersistence bankInfoPersistence, PayoutGateway payoutGateway) {
        this.bankInfoPersistence = Objects.requireNonNull(bankInfoPersistence);
        this.payoutGateway = Objects.requireNonNull(payoutGateway);
    }

    /**
     * Creates or updates the user's bank account on Stripe Connect.
     * If the user already has a Connect account registered, the existing account is updated
     * with the new bank details instead of creating a duplicate.
     *
     * @param userId    The user's ID.
     * @param userEmail The user's email (required to create the Connect account on Stripe).
     * @param bankName  The account holder name.
     * @param iban      The IBAN.
     * @param bic       The BIC/SWIFT code.
     * @return The saved BankInfo with the Stripe Connect account ID.
     */
    public BankInfo createOrUpdateBankInfo(UUID userId, String userEmail,
                                            String bankName, String iban, String bic) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(userEmail);
        Objects.requireNonNull(bankName);
        Objects.requireNonNull(iban);
        Objects.requireNonNull(bic);

        // Reuse existing Connect account if the user already has one
        var existingConnectAccountId = bankInfoPersistence.findByUserId(userId)
                .map(BankInfo::externalAccountId)
                .orElse(null);

        var connectAccountId = payoutGateway.createExternalAccount(
                userId, userEmail, bankName, iban, bic, existingConnectAccountId);

        BankInfo bankInfo = new BankInfo(userId, bankName, connectAccountId);
        return bankInfoPersistence.saveBankInfo(bankInfo, userId);
    }

    public Optional<BankInfo> getBankInfoByUserId(UUID userId) {
        Objects.requireNonNull(userId);
        return bankInfoPersistence.findByUserId(userId);
    }

    public void deleteBankInfo(UUID bankInfoId) {
        Objects.requireNonNull(bankInfoId);
        bankInfoPersistence.deleteBankInfo(bankInfoId);
    }
}
