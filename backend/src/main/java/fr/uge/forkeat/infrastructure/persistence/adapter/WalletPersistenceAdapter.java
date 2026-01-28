package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.mapper.TransactionMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.mapper.WalletMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.TransactionJpaRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.WalletJpaRepository;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.Transaction;
import fr.uge.forkeat.service.model.Wallet;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class WalletPersistenceAdapter implements WalletPersistence {

    private final WalletJpaRepository walletRepository;
    private final TransactionJpaRepository transactionRepository;

    // LES MAPPERS
    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;

    public WalletPersistenceAdapter(WalletJpaRepository walletRepository, 
                                    TransactionJpaRepository transactionRepository, WalletMapper walletMapper, TransactionMapper transactionMapper) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.walletMapper = walletMapper;
        this.transactionMapper = transactionMapper;

    }

    @Override
    public Optional<Wallet> loadWalletWithLock(UUID userId) {
        return walletRepository.findByUserId(userId)
                .map(walletMapper::toDomain);
    }

    @Override
    public Optional<Wallet> getWalletById(UUID walletId) {
        return walletRepository.findById(walletId).map(walletMapper::toDomain);
    }

    @Override
    public Wallet saveWallet(Wallet wallet) throws ResourceNotFoundException {
        var entity = walletRepository.findById(wallet.id())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet introuvable"));

        walletMapper.updateEntity(entity, wallet); // Pour éviter de crée un nouvuea objet
        return walletMapper.toDomain(entity);
    }

    @Override
    public boolean transactionExists(String externalId) {
        return transactionRepository.existsByStripeTransactionID(externalId);
    }

    @Override
    public Transaction saveTransaction(Transaction transactionDomain) {
        var entity = transactionMapper.toEntity(transactionDomain);

        if (transactionDomain.walletDestinationId() != null) {
            var dest = walletRepository.getReferenceById(transactionDomain.walletDestinationId());
            entity.setDestinationWallet(dest);
        }

        if (transactionDomain.walletSourceId() != null) {
            var source = walletRepository.getReferenceById(transactionDomain.walletSourceId());
            entity.setSourceWallet(source);
        }

        return transactionMapper.toDomain(transactionRepository.save(entity));
    }

    @Override
    public Optional<WalletEntity> findByUserId(UUID userId) {
        return walletRepository.findByUserId(userId);
    }
}