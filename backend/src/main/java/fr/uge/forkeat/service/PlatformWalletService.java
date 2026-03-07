package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.wallet.PlatformWallet;
import fr.uge.forkeat.service.model.wallet.PlatformWalletTransaction;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import fr.uge.forkeat.service.persistence.PlatformWalletPersistence;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class PlatformWalletService {

    private final PlatformWalletPersistence platformWalletPersistence;

    public PlatformWalletService(PlatformWalletPersistence platformWalletPersistence) {
        this.platformWalletPersistence = Objects.requireNonNull(platformWalletPersistence);
    }

    @Transactional(readOnly = true)
    public PlatformWallet getWallet(PlatformWalletType type) {
        Objects.requireNonNull(type);
        return platformWalletPersistence.findByType(type)
                .orElseThrow(() -> new ResourceNotFoundException("Platform wallet not found: " + type));
    }

    @Transactional(readOnly = true)
    public long getBalance(PlatformWalletType type) {
        return platformWalletPersistence.balance(type);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 30)
    public PlatformWallet credit(PlatformWalletType type, long amount) {
        Objects.requireNonNull(type);
        if (amount <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        var wallet = platformWalletPersistence.findByTypeWithLock(type)
                .orElseThrow(() -> new ResourceNotFoundException("Platform wallet not found: " + type));
        var updated = new PlatformWallet(wallet.id(), wallet.type(), wallet.balance() + amount, Instant.now());
        return platformWalletPersistence.save(updated);
    }

    @Transactional(readOnly = true)
    public List<PlatformWalletTransaction> getTransactionHistory() {
        return platformWalletPersistence.findAllTransactionsDesc();
    }
}
