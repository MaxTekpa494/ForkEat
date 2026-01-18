package fr.uge.forkeat.service;

import fr.uge.forkeat.service.model.Transaction;
import fr.uge.forkeat.service.model.TransactionType;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.presentation.rest.dto.PaymentRequest;
import fr.uge.forkeat.service.model.Wallet;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import fr.uge.forkeat.service.external.PaymentGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class WalletService {

    private final PaymentGateway paymentGateway;
    private final WalletPersistence walletPersistence;

    public WalletService(PaymentGateway paymentGateway,
                         WalletPersistence walletPersistence) {
        this.paymentGateway = paymentGateway;
        this.walletPersistence = walletPersistence;
    }

    public String prepareTopUp(Long userId, String email, Long amount, String currency) {
        var request = new PaymentRequest(userId, email, amount, currency);
        return paymentGateway.initiatePayment(request).paymentUrl();
    }

    @Transactional
    public void processPaymentConfirmation(Long userId, Long amount, String stripeTransactionId) throws ResourceNotFoundException {

        // IDEMPOTENCE
        if (walletPersistence.transactionExists(stripeTransactionId)) {
            return;
        }

        // VERROUILLAGE PESSIMISTE
        var wallet = walletPersistence.loadWalletWithLock(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet introuvable pour user " + userId));

        var newWallet = wallet.withBalance(wallet.balance() + amount);
        walletPersistence.saveWallet(newWallet);

        // TRACABILITÉ
        var trace = new Transaction(null, wallet.id(),
                                    amount, TransactionType.RECHARGE,
                                    Instant.now(), stripeTransactionId);

        walletPersistence.saveTransaction(trace);
    }

    @Transactional(readOnly = true)
    public Long getBalance(Long userId) {
        return walletPersistence.loadWalletWithLock(userId)
                .map(Wallet::balance)
                .orElse(0L);
    }
}