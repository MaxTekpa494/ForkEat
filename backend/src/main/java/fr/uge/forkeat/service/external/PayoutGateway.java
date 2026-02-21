package fr.uge.forkeat.service.external;

import fr.uge.forkeat.service.model.Currency;
import java.util.UUID;

public interface PayoutGateway {

    /**
     * Initiates a payout to a user's bank account via Stripe Connect.
     * Creates a Transfer from the platform to the user's Connect Custom Account,
     * then triggers an immediate Payout from that Connect account to the bank.
     *
     * @param userId           The ID of the user requesting the payout.
     * @param amount           The amount in cents.
     * @param connectAccountId The Stripe Connect Custom Account ID (acct_xxx) for the user.
     * @param currency         The currency of the payout.
     * @return The Stripe Transfer ID (tr_xxx).
     */
    String initiatePayout(UUID userId, UUID pendingTxId, Long amount, String connectAccountId, Currency currency);

    /**
     * Creates (or updates) a Stripe Connect Custom Account for the user and attaches their
     * bank account. The user never interacts with Stripe directly — this is transparent.
     *
     * @param userId                   The ID of the user.
     * @param userEmail                The user's email (used to create the Connect account).
     * @param bankName                 The account holder name.
     * @param iban                     The IBAN.
     * @param bic                      The BIC/SWIFT code.
     * @param existingConnectAccountId The existing Connect account ID if updating, null if creating.
     * @return The Stripe Connect Account ID (acct_xxx).
     */
    String createExternalAccount(UUID userId, String userEmail, String bankName,
                                  String iban, String bic, String existingConnectAccountId);
}
