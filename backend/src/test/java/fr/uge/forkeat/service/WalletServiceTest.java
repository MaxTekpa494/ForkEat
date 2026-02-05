package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.DuplicateTransactionException;
import fr.uge.forkeat.service.exception.WalletNotFoundException;
import fr.uge.forkeat.service.model.PaymentRequest;
import fr.uge.forkeat.service.model.PaymentResponse;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.external.PaymentGateway;
import fr.uge.forkeat.service.model.Transaction;
import fr.uge.forkeat.service.model.user.Wallet;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private PaymentGateway paymentGateway;
    @Mock private WalletPersistence walletPersistence;

    @InjectMocks
    private WalletService walletService;

    @Test
    void prepareTopUp_ShouldReturnUrl() {
        var userId = UUID.randomUUID();
        var expectedUrl = "https://stripe.com/pay/123";
        
        var mockResponse = mock(PaymentResponse.class);
        when(mockResponse.paymentUrl()).thenReturn(expectedUrl);
        when(paymentGateway.initiatePayment(any(PaymentRequest.class))).thenReturn(mockResponse);

        String resultUrl = walletService.prepareTopUp(userId, "test@test.com", 1000L);

        assertEquals(expectedUrl, resultUrl);
    }

    @Test
    void processPaymentConfirmation_ShouldCreditWallet_WhenNewTransaction() throws ResourceNotFoundException {
        UUID userId = UUID.randomUUID();
        Long amount = 500L;
        String stripeId = "tx_12345";
        
        Wallet initialWallet = new Wallet(UUID.randomUUID(), userId, 1000, Instant.now());
        when(walletPersistence.transactionExists(stripeId)).thenReturn(false);
        when(walletPersistence.loadWalletWithLock(userId)).thenReturn(Optional.of(initialWallet));

        walletService.processPaymentConfirmation(userId, amount, stripeId);

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletPersistence).saveWallet(walletCaptor.capture());
        
        assertEquals(1500L, walletCaptor.getValue().balance(), "Le solde doit être 1000 + 500");

        verify(walletPersistence).saveTransaction(any(Transaction.class));
    }

    @Test
    void processPaymentConfirmation_ShouldDoNothing_WhenTransactionAlreadyExists() throws ResourceNotFoundException {
        String stripeId = "tx_déjà_traité";
        when(walletPersistence.transactionExists(stripeId)).thenReturn(true);

        assertThrows(DuplicateTransactionException.class, () -> {
            walletService.processPaymentConfirmation(UUID.randomUUID(), 100L, stripeId);
        });

        verify(walletPersistence, never()).loadWalletWithLock(any());
        verify(walletPersistence, never()).saveWallet(any());
    }

    @Test
    void processPaymentConfirmation_ShouldThrow_WhenWalletNotFound() {
        UUID userId = UUID.randomUUID();
        when(walletPersistence.transactionExists(anyString())).thenReturn(false);
        when(walletPersistence.loadWalletWithLock(userId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () ->
            walletService.processPaymentConfirmation(userId, 100L, "tx_123")
        );
    }
}