package fr.uge.forkeat.infrastructure.payment.adapter;

import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.Token;
import com.stripe.model.Transfer;
import fr.uge.forkeat.infrastructure.config.StripeProperties;
import fr.uge.forkeat.service.exception.PaymentException;
import fr.uge.forkeat.service.external.PayoutGateway;
import fr.uge.forkeat.service.model.wallet.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StripePayoutGatewayAdapter implements PayoutGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePayoutGatewayAdapter.class);
    private final StripeProperties stripeProperties;

    public StripePayoutGatewayAdapter(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
    }

    @Override
    public String createExternalAccount(UUID userId,
                                        String userEmail,
                                        String bankName,
                                        String iban,
                                        String bic,
                                        String existingConnectAccountId) {
        try {
            boolean hasRealAccount = existingConnectAccountId != null
                    && existingConnectAccountId.startsWith("acct_")
                    && !existingConnectAccountId.startsWith("acct_simulated");

            if (hasRealAccount) {
                attachBankAccount(existingConnectAccountId, bankName, iban);
                return existingConnectAccountId;
            }

            String accountId = createConnectAccount(userEmail);
            attachBankAccount(accountId, bankName, iban);

            return accountId;
        } catch (StripeException e) {
            throw new PaymentException("Stripe Connect setup failed: " + e.getMessage(), e);
        }
    }

    private String createConnectAccount(String userEmail) throws StripeException {
        var individual = buildIndividual(userEmail);

        Map<String, Object> accountTokenData = new HashMap<>();
        accountTokenData.put("business_type", stripeProperties.connect().businessType());
        accountTokenData.put("individual", individual);
        accountTokenData.put("tos_shown_and_accepted", stripeProperties.connect().tosAccepted());

        Token accountToken = Token.create(Map.of("account", accountTokenData));

        Map<String, Object> params = new HashMap<>();
        params.put("type", "custom");
        params.put("country", stripeProperties.connect().country());
        params.put("email", userEmail);
        params.put("account_token", accountToken.getId());
        params.put("business_profile", Map.of(
                "url", "https://accessible.stripe.com",
                "mcc", stripeProperties.connect().mcc()));
        params.put("capabilities", Map.of("transfers", Map.of("requested", true)));

        Account account = Account.create(params);
        logAccountStatus(account);
        return account.getId();
    }

    private Map<String, Object> buildIndividual(String userEmail) {
        var dobConfig = stripeProperties.test().dob();
        var addressConfig = stripeProperties.test().address();
        var documentConfig = stripeProperties.test().document();

        Map<String, Object> individual = new HashMap<>();
        individual.put("first_name", "Test");
        individual.put("last_name", "ForkEat");
        individual.put("email", userEmail);
        individual.put("phone", "0000000000");

        individual.put("dob", Map.of(
                "day", dobConfig.day(),
                "month", dobConfig.month(),
                "year", dobConfig.year()
        ));

        individual.put("address", Map.of(
                "line1", addressConfig.line1(),
                "city", addressConfig.city(),
                "postal_code", addressConfig.postalCode(),
                "country", addressConfig.country()
        ));

        individual.put("verification", Map.of("document",
                                                     Map.of("front", documentConfig.front())
                                                 ));
        return individual;
    }

    private void attachBankAccount(String accountId,
                                   String bankName,
                                   String iban) throws StripeException {
        Map<String, Object> bankAccountData = Map.of(
                "object", "bank_account",
                "country", stripeProperties.connect().country(),
                "currency", "eur",
                "account_holder_name", bankName,
                "account_holder_type", "individual",
                "account_number", iban
        );

        Account account = Account.retrieve(accountId);
        account.getExternalAccounts()
                .create(Map.of("external_account", bankAccountData));
    }

    @Override
    public String initiatePayout(UUID userId,
                                 UUID pendingTxId,
                                 Long amount,
                                 String connectAccountId,
                                 Currency currency) {
        try {
            Map<String, Object> params = Map.of(
                    "amount", amount,
                    "currency", currency.code(),
                    "destination", connectAccountId,
                    "metadata", Map.of(
                            "userId", userId.toString(),
                            "pendingTxId", pendingTxId.toString()
                    )
            );

            Transfer transfer = Transfer.create(params);
            return transfer.getId();

        } catch (StripeException e) {
            throw new PaymentException("Stripe transfer failed: " + e.getMessage(), e);
        }
    }

    private void logAccountStatus(Account account) {
        if (account.getCapabilities() != null) {
            log.info("Account {} transfers capability: {}",
                    account.getId(),
                    account.getCapabilities().getTransfers());
        }
    }
}
