package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.Transaction;
import fr.uge.forkeat.service.model.wallet.Wallet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletPersistence {
    Optional<Wallet> loadWalletWithLock(UUID userId);
    Optional<Wallet> getWalletById(UUID walletId);
    Wallet saveWallet(Wallet wallet) throws ResourceNotFoundException;
    boolean transactionExists(String externalId);
    Transaction saveTransaction(Transaction transaction);
    Optional<Wallet> findByUserId(UUID userId) throws ResourceNotFoundException;
    Long getBalance(UUID userId);
    List<Transaction> getTransactionsByUserId(UUID userId);
    Optional<Transaction> findTransactionByStripeTransactionID(String stripeTransactionID);
    Optional<Transaction> findTransactionById(UUID id);
    Transaction updateTransaction(Transaction transaction);
}
