package fr.uge.forkeat.infrastructure.persistence.payment.adapter;

import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.ExternalAccountCollection;
import com.stripe.model.Transfer;
import fr.uge.forkeat.infrastructure.payment.adapter.StripePayoutGatewayAdapter;
import fr.uge.forkeat.infrastructure.config.StripeProperties;
import fr.uge.forkeat.service.exception.PaymentException;
import fr.uge.forkeat.service.model.wallet.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StripePayoutGatewayAdapterTest {

    private StripePayoutGatewayAdapter adapter;

    private UUID userId;
    private UUID pendingTxId;
    private Long amount;
    private String existingAccountId;
    private String bankName;
    private String iban;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        pendingTxId = UUID.randomUUID();
        amount = 1000L;
        existingAccountId = "acct_existing_123"; // starts with "acct_" but not "acct_simulated" → hasRealAccount=true
        bankName = "Example Bank";
        iban = "FR1234567890123456789012345";

        // Use a real StripeProperties instance (records can be constructed directly)
        var connect = new StripeProperties.Connect("FR", "5814", "individual", true);
        var stripeProperties = new StripeProperties(connect, null);
        adapter = new StripePayoutGatewayAdapter(stripeProperties);
    }

    // ──── initiatePayout ────────────────────────────────────────────────────

    @Test
    void initiatePayout_shouldReturnPayoutIdOnSuccess() throws StripeException {
        try (MockedStatic<Transfer> mockedTransfer = Mockito.mockStatic(Transfer.class)) {
            Transfer mockTransfer = mock(Transfer.class);
            when(mockTransfer.getId()).thenReturn("tr_test_123");
            mockedTransfer.when(() -> Transfer.create(any(Map.class))).thenReturn(mockTransfer);

            String result = adapter.initiatePayout(userId, pendingTxId, amount, existingAccountId, Currency.EUR);

            assertEquals("tr_test_123", result);
        }
    }

    @Test
    void initiatePayout_shouldThrowPaymentException_OnStripeError() {
        try (MockedStatic<Transfer> mockedTransfer = Mockito.mockStatic(Transfer.class)) {
            StripeException mockException = mock(StripeException.class);
            mockedTransfer.when(() -> Transfer.create(any(Map.class))).thenThrow(mockException);

            assertThrows(PaymentException.class, () ->
                    adapter.initiatePayout(userId, pendingTxId, amount, existingAccountId, Currency.EUR));
        }
    }

    // ──── createExternalAccount ─────────────────────────────────────────────
    // Tests use an existing connect account ID ("acct_existing_123") which triggers
    // the attachBankAccount path, bypassing Token.create / Account.create.

    @Test
    void createExternalAccount_shouldReturnExistingAccountId_OnSuccess() throws StripeException {
        try (MockedStatic<Account> mockedAccount = Mockito.mockStatic(Account.class)) {
            ExternalAccountCollection mockExtAccounts = mock(ExternalAccountCollection.class);
            when(mockExtAccounts.create(any(Map.class))).thenReturn(null);

            Account mockAccount = mock(Account.class);
            when(mockAccount.getExternalAccounts()).thenReturn(mockExtAccounts);

            mockedAccount.when(() -> Account.retrieve(existingAccountId)).thenReturn(mockAccount);

            String result = adapter.createExternalAccount(
                    userId, "user@test.com", bankName, iban, "BNPAFRPP", existingAccountId);

            assertEquals(existingAccountId, result);
        }
    }

    @Test
    void createExternalAccount_shouldThrowPaymentException_OnStripeError() {
        try (MockedStatic<Account> mockedAccount = Mockito.mockStatic(Account.class)) {
            mockedAccount.when(() -> Account.retrieve(existingAccountId))
                    .thenThrow(mock(StripeException.class));

            assertThrows(PaymentException.class, () ->
                    adapter.createExternalAccount(
                            userId, "user@test.com", bankName, iban, "BNPAFRPP", existingAccountId));
        }
    }
}
