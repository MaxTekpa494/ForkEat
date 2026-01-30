package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.DuplicateTransactionException;
import fr.uge.forkeat.service.exception.WalletNotFoundException;
import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import fr.uge.forkeat.service.external.PaymentGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class WalletService {

    private final PaymentGateway paymentGateway;
    private final WalletPersistence walletPersistence;

    public WalletService(PaymentGateway paymentGateway,
                         WalletPersistence walletPersistence) {
        this.paymentGateway = paymentGateway;
        this.walletPersistence = walletPersistence;
    }

    public String prepareTopUp(UUID userId, String email, Long amount) {
        var request = new PaymentRequest(userId, email, amount, Currency.DEFAULT.code());
        return paymentGateway.initiatePayment(request).paymentUrl();
    }

    @Transactional
    public void processPaymentConfirmation(UUID userId, Long amount, String stripeTransactionID) throws ResourceNotFoundException {

        // IDEMPOTENCE
        if (walletPersistence.transactionExists(stripeTransactionID)) {
            throw new DuplicateTransactionException(stripeTransactionID);
        }

        // VERROUILLAGE PESSIMISTE
        var wallet = walletPersistence.loadWalletWithLock(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));

        var newWallet = wallet.addFunds(amount);
        walletPersistence.saveWallet(newWallet);

        // TRACABILITÉ
        var trace = new Transaction(null, wallet.id(),
                                    amount, TransactionType.RECHARGE,
                                    Instant.now(), stripeTransactionID);

        walletPersistence.saveTransaction(trace);
    }

    @Transactional(readOnly = true)
    public Long getBalance(UUID userId) {
        return walletPersistence.getBalance(userId);
    }

    /**
     * Crée un nouveau wallet pour un utilisateur.
     */
    @Transactional
    public Wallet createWallet(UUID userId) throws ResourceNotFoundException {
        var newWallet = new Wallet(UUID.randomUUID(), 0L, userId, Instant.now());
        return walletPersistence.saveWallet(newWallet);
    }
}