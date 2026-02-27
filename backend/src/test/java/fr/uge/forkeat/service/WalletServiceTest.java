package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.DuplicateTransactionException;
import fr.uge.forkeat.service.exception.PaymentException;
import fr.uge.forkeat.service.exception.WalletNotFoundException;
import fr.uge.forkeat.service.exception.WithdrawalException;
import fr.uge.forkeat.service.external.PaymentGateway;
import fr.uge.forkeat.service.external.PayoutGateway;
import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.model.user.BankInfo;
import fr.uge.forkeat.service.model.wallet.Currency;
import fr.uge.forkeat.service.model.wallet.Wallet;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import fr.uge.forkeat.service.user.BankInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private PaymentGateway paymentGateway;
    @Mock private PayoutGateway payoutGateway;
    @Mock private WalletPersistence walletPersistence;
    @Mock private BankInfoService bankInfoService;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private TransactionStatus transactionStatus;
    @Mock private org.springframework.transaction.TransactionStatus springTransactionStatus;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        // Configuration du TransactionManager pour que le TransactionTemplate fonctionne en test unitaire
        // Cela permet d'exécuter le code à l'intérieur des lambdas txTemplate.execute()
        lenient().when(transactionManager.getTransaction(any())).thenReturn(springTransactionStatus);

        walletService = new WalletService(
                paymentGateway,
                payoutGateway,
                walletPersistence,
                bankInfoService,
                transactionManager
        );
    }

    @Nested
    @DisplayName("prepareTopUp")
    class PrepareTopUpTests {
        @Test
        void shouldReturnPaymentUrl() {
            // Given
            var userId = UUID.randomUUID();
            var email = "test@example.com";
            var amount = 1000L;
            var source = "cb";
            var expectedUrl = "https://stripe.com/pay/123";

            when(paymentGateway.initiatePayment(any(PaymentRequest.class)))
                    .thenReturn(new PaymentResponse(expectedUrl, "id_123"));

            // When
            var result = walletService.prepareTopUp(userId, email, amount, source);

            // Then
            assertThat(result).isEqualTo(expectedUrl);
            verify(paymentGateway).initiatePayment(argThat(req ->
                    req.userId().equals(userId) && req.amount().equals(amount)
            ));
        }
    }

    @Nested
    @DisplayName("processPaymentConfirmation")
    class ProcessPaymentConfirmationTests {

        @Test
        void shouldCreditWallet_WhenSuccess() {
            // Given
            var userId = UUID.randomUUID();
            var initialBalance = 500L;
            var topUpAmount = 1000L;
            var stripeId = "txn_stripe_123";

            var wallet = new Wallet(UUID.randomUUID(), userId, initialBalance, Instant.now());

            when(walletPersistence.transactionExists(stripeId)).thenReturn(false);
            when(walletPersistence.loadWalletWithLock(userId)).thenReturn(Optional.of(wallet));

            // When
            walletService.processPaymentConfirmation(userId, topUpAmount, stripeId);

            // Then
            var walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletPersistence).saveWallet(walletCaptor.capture());

            assertThat(walletCaptor.getValue().balance()).isEqualTo(1500L); // 500 + 1000

            verify(walletPersistence).saveTransaction(argThat(tx ->
                    tx.amount().equals(topUpAmount) &&
                            tx.type() == TransactionType.RECHARGE &&
                            tx.status() == TransactionStatus.SUCCEEDED
            ));
        }

        @Test
        void shouldThrowException_WhenTransactionDuplicate() {
            // Given
            var stripeId = "txn_duplicate";
            when(walletPersistence.transactionExists(stripeId)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> walletService.processPaymentConfirmation(UUID.randomUUID(), 100L, stripeId))
                    .isInstanceOf(DuplicateTransactionException.class);

            verify(walletPersistence, never()).saveWallet(any());
        }

        @Test
        void shouldThrowException_WhenWalletNotFound() {
            // Given
            var userId = UUID.randomUUID();
            var stripeId = "txn_123";
            when(walletPersistence.transactionExists(stripeId)).thenReturn(false);
            when(walletPersistence.loadWalletWithLock(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> walletService.processPaymentConfirmation(userId, 100L, stripeId))
                    .isInstanceOf(WalletNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("requestWithdrawal")
    class RequestWithdrawalTests {

        @Test
        void shouldCompleteWithdrawal_WhenAllStepsSucceed() {
            // Given
            var userId = UUID.randomUUID();
            var currentBalance = 2000L;
            var withdrawAmount = 500L;
            var walletId = UUID.randomUUID();
            var stripeAccountId = "acct_123";
            var stripePayoutId = "po_123";

            var wallet = new Wallet(walletId, userId, currentBalance, Instant.now());
            var bankInfo = new BankInfo(userId, "Bank", stripeAccountId);

            when(bankInfoService.getBankInfoByUserId(userId)).thenReturn(Optional.of(bankInfo));
            when(walletPersistence.loadWalletWithLock(userId)).thenReturn(Optional.of(wallet));

            // Simulation du débit du wallet
            when(walletPersistence.saveWallet(any(Wallet.class))).thenAnswer(i -> i.getArguments()[0]);

            when(payoutGateway.initiatePayout(any(), any(), any(), any(), any())).thenReturn(stripePayoutId);

            // When
            String result = walletService.requestWithdrawal(userId, withdrawAmount);

            // Then
            assertThat(result).isEqualTo(stripePayoutId);

            // Verif Phase 1: Débit
            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletPersistence, atLeastOnce()).saveWallet(walletCaptor.capture());
            // Le premier appel à saveWallet doit être le débit
            assertThat(walletCaptor.getAllValues().getFirst().balance()).isEqualTo(1500L);

            // Verif Phase 1: Pending TX
            verify(walletPersistence).saveTransaction(argThat(tx ->
                    tx.status() == TransactionStatus.PENDING &&
                            tx.type() == TransactionType.WITHDRAWAL
            ));

            // Verif Phase 2: Appel Stripe avec le pendingTxId en metadata
            verify(payoutGateway).initiatePayout(eq(userId), any(UUID.class), eq(withdrawAmount), eq(stripeAccountId), eq(Currency.EUR));

            // Pas de updateTransaction ici : c'est le webhook (linkAndConfirmPayout) qui met à jour
            verify(walletPersistence, never()).updateTransaction(any());
        }

        @Test
        void shouldThrowException_WhenInsufficientBalance() {
            // Given
            UUID userId = UUID.randomUUID();
            long currentBalance = 100L;
            Long withdrawAmount = 500L;

            Wallet wallet = new Wallet(UUID.randomUUID(), userId, currentBalance, Instant.now());
            BankInfo bankInfo = new BankInfo(userId,  "Bank", "acct_123");

            when(bankInfoService.getBankInfoByUserId(userId)).thenReturn(Optional.of(bankInfo));
            when(walletPersistence.loadWalletWithLock(userId)).thenReturn(Optional.of(wallet));

            // When/Then
            assertThatThrownBy(() -> walletService.requestWithdrawal(userId, withdrawAmount))
                    .isInstanceOf(WithdrawalException.class)
                    .hasMessageContaining("Insufficient balance");

            verify(payoutGateway, never()).initiatePayout(any(), any(), any(), any(), any());
        }

        @Test
        void shouldThrowPaymentException_WhenStripeCallFails() {
            // Given
            UUID userId = UUID.randomUUID();
            Long amount = 500L;
            Wallet wallet = new Wallet(UUID.randomUUID(), userId, 1000L, Instant.now());
            BankInfo bankInfo = new BankInfo(userId, "Bank", "acct_123");

            when(bankInfoService.getBankInfoByUserId(userId)).thenReturn(Optional.of(bankInfo));
            when(walletPersistence.loadWalletWithLock(userId)).thenReturn(Optional.of(wallet));
            when(walletPersistence.saveWallet(any(Wallet.class))).thenAnswer(i -> i.getArguments()[0]);

            // Le gateway échoue
            doThrow(new PaymentException("Stripe error")).when(payoutGateway).initiatePayout(any(), any(), any(), any(), any());

            // When/Then : PaymentException se propage directement
            assertThatThrownBy(() -> walletService.requestWithdrawal(userId, amount))
                    .isInstanceOf(PaymentException.class);

            // Seul le débit a eu lieu (la compensation est gérée par webhook)
            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletPersistence, times(1)).saveWallet(walletCaptor.capture());
            assertThat(walletCaptor.getValue().balance()).isEqualTo(500L);

            // Pas de updateTransaction synchrone
            verify(walletPersistence, never()).updateTransaction(any());
        }
    }

    @Nested
    @DisplayName("processPayoutConfirmation")
    class ProcessPayoutConfirmationTests {

        @Test
        void shouldMarkSucceeded_WhenCallbackIsSuccess() {
            // Given
            String payoutId = "po_123";
            Transaction tx = new Transaction(UUID.randomUUID(), UUID.randomUUID(), null, 500L, TransactionType.WITHDRAWAL, Instant.now(), payoutId, TransactionStatus.PENDING);

            when(walletPersistence.findTransactionByStripeTransactionID(payoutId)).thenReturn(Optional.of(tx));

            // When
            walletService.processPayoutConfirmation(payoutId, true);

            // Then
            verify(walletPersistence).updateTransaction(argThat(t -> t.status() == TransactionStatus.SUCCEEDED));
            verify(walletPersistence, never()).saveWallet(any()); // Pas de remboursement
        }

        @Test
        void shouldRevertFunds_WhenCallbackIsFailure() {
            // Given
            String payoutId = "po_fail";
            UUID walletId = UUID.randomUUID();
            Long amount = 500L;

            Transaction tx = new Transaction(UUID.randomUUID(), walletId, null, amount, TransactionType.WITHDRAWAL, Instant.now(), payoutId, TransactionStatus.PENDING);
            Wallet wallet = new Wallet(walletId, UUID.randomUUID(), 0L, Instant.now());

            when(walletPersistence.findTransactionByStripeTransactionID(payoutId)).thenReturn(Optional.of(tx));
            when(walletPersistence.getWalletById(walletId)).thenReturn(Optional.of(wallet));

            // When
            walletService.processPayoutConfirmation(payoutId, false);

            // Then
            verify(walletPersistence).updateTransaction(argThat(t -> t.status() == TransactionStatus.FAILED));

            // Vérifier le remboursement
            verify(walletPersistence).saveWallet(argThat(w -> w.balance() == amount));
        }
    }

    @Test
    void createWallet_ShouldSaveNewWallet() {
        // Given
        UUID userId = UUID.randomUUID();
        when(walletPersistence.saveWallet(any(Wallet.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        Wallet result = walletService.createWallet(userId);

        // Then
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.balance()).isZero();
        verify(walletPersistence).saveWallet(any(Wallet.class));
    }
}