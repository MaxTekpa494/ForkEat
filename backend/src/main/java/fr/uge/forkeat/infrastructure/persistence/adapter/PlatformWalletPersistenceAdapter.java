package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.PlatformWalletEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.PlatformWalletRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.WalletRepository;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.wallet.PlatformWallet;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import fr.uge.forkeat.service.persistence.PlatformWalletPersistence;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class PlatformWalletPersistenceAdapter implements PlatformWalletPersistence {

    private final PlatformWalletRepository platformWalletRepository;
    private final WalletRepository walletRepository;

    public PlatformWalletPersistenceAdapter(PlatformWalletRepository platformWalletRepository,
                                             WalletRepository walletRepository) {
        this.platformWalletRepository = Objects.requireNonNull(platformWalletRepository);
        this.walletRepository = Objects.requireNonNull(walletRepository);
    }

    @Override
    public Optional<PlatformWallet> findByType(PlatformWalletType type) {
        Objects.requireNonNull(type);
        return platformWalletRepository.findByType(type).map(PlatformWalletEntityMapper::toDomain);
    }

    @Override
    public Optional<PlatformWallet> findByTypeWithLock(PlatformWalletType type) {
        Objects.requireNonNull(type);
        return platformWalletRepository.findByTypeWithLock(type).map(PlatformWalletEntityMapper::toDomain);
    }

    @Override
    public PlatformWallet save(PlatformWallet wallet) {
        Objects.requireNonNull(wallet);
        var entity = platformWalletRepository.findById(wallet.id())
                .orElseThrow(() -> new ResourceNotFoundException("Platform wallet not found: " + wallet.id()));
        // Update balance in the associated wallet
        var walletEntity = entity.getWallet();
        walletEntity.setBalance(wallet.balance());
        walletRepository.save(walletEntity);
        return PlatformWalletEntityMapper.toDomain(entity);
    }

    @Override
    public List<PlatformWallet> findAll() {
        return platformWalletRepository.findAll().stream()
                .map(PlatformWalletEntityMapper::toDomain)
                .toList();
    }

    @Override
    public long balance(PlatformWalletType type) {
        Objects.requireNonNull(type);
        var wallet = platformWalletRepository.findByType(type);

        if(wallet.isEmpty()){
            return 0L;
        }
        return wallet.get().getWallet().getBalance();
    }
}
