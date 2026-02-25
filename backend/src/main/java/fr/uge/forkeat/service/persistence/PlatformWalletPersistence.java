package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.wallet.PlatformWallet;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;

import java.util.List;
import java.util.Optional;

public interface PlatformWalletPersistence {
    Optional<PlatformWallet> findByType(PlatformWalletType type);
    PlatformWallet save(PlatformWallet wallet);
    List<PlatformWallet> findAll();
}
