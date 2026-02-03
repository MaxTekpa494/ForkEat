package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.Transaction;
import fr.uge.forkeat.service.model.user.Wallet;

import java.util.Optional;
import java.util.UUID;

public interface WalletPersistence {
    // Récupérer par ID Utilisateur (avec verrou pour modification)
    Optional<Wallet> loadWalletWithLock(UUID userId);

    // Récupérer par ID du Wallet (lecture simple)
    Optional<Wallet> getWalletById(UUID walletId);

    Wallet saveWallet(Wallet wallet) throws ResourceNotFoundException;

    boolean transactionExists(String externalId);

    Transaction saveTransaction(Transaction transaction);

    Optional<Wallet> findByUserId(UUID userId) throws ResourceNotFoundException;

    Long getBalance(UUID userId);
}