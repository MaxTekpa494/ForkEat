package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.TransactionEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.Transaction;
import fr.uge.forkeat.service.model.Wallet;

import java.util.Optional;


public interface WalletPersistence {
    // Récupérer par ID Utilisateur (avec verrou pour modification)
    Optional<Wallet> loadWalletWithLock(Long userId);

    // Récupérer par ID du Wallet (lecture simple)
    Optional<Wallet> getWalletById(Long walletId);

    Wallet saveWallet(Wallet wallet) throws ResourceNotFoundException;

    boolean transactionExists(String externalId);

    Transaction saveTransaction(Transaction transaction);

    Optional<WalletEntity> findByUserId(Long userId);
}