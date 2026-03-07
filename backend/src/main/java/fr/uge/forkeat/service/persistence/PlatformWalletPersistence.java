package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.wallet.PlatformWallet;
import fr.uge.forkeat.service.model.wallet.PlatformWalletTransaction;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformWalletPersistence {
    Optional<PlatformWallet> findByType(PlatformWalletType type);
    Optional<PlatformWallet> findByTypeWithLock(PlatformWalletType type);
    PlatformWallet save(PlatformWallet wallet);
    List<PlatformWallet> findAll();
    long balance(PlatformWalletType type);
    void recordTransaction(PlatformWalletType walletType, long amountCents, String reason, UUID referenceId);
    List<PlatformWalletTransaction> findAllTransactionsDesc();
}
