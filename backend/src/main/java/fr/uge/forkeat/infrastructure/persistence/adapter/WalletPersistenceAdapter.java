package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.infrastructure.persistence.mapper.TransactionEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.mapper.WalletEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.TransactionRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.WalletRepository;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.Transaction;
import fr.uge.forkeat.service.model.user.Wallet;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class WalletPersistenceAdapter implements WalletPersistence {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    // LES MAPPERS
    private final WalletEntityMapper walletEntityMapper;
    private final TransactionEntityMapper transactionEntityMapper;

    public WalletPersistenceAdapter(WalletRepository walletRepository, TransactionRepository transactionRepository,
            UserRepository userRepository, WalletEntityMapper walletEntityMapper,
            TransactionEntityMapper transactionEntityMapper) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.walletEntityMapper = walletEntityMapper;
        this.transactionEntityMapper = transactionEntityMapper;
    }

    @Override
    public Optional<Wallet> loadWalletWithLock(UUID userId) {
        return walletRepository.findByUserId(userId).map(walletEntityMapper::toDomain);
    }

    @Override
    public Optional<Wallet> getWalletById(UUID walletId) {
        return walletRepository.findById(walletId).map(walletEntityMapper::toDomain);
    }

    @Override
    public Wallet saveWallet(Wallet wallet) {
        var entity = walletRepository.findById(wallet.id()) // En faisant ça, on donne la possibilité à wallet de
                                                            // fonctionner sans user
                .orElseGet(() -> {
                    var newEntity = new WalletEntity();
                    newEntity.setId(wallet.id());

                    var userEntity = userRepository.findById(wallet.userId())
                            .orElseThrow(() -> new IllegalStateException("User not found: " + wallet.userId()));
                    userEntity.setWallet(newEntity);

                    return newEntity;
                });

        walletEntityMapper.updateEntity(entity, wallet);
        var saved = walletRepository.save(entity);
        return walletEntityMapper.toDomain(saved);
    }

    @Override
    public boolean transactionExists(String externalId) {
        return transactionRepository.existsByStripeTransactionID(externalId);
    }

    @Override
    public Transaction saveTransaction(Transaction transactionDomain) {
        var entity = transactionEntityMapper.toEntity(transactionDomain);

        if (transactionDomain.walletDestinationId() != null) {
            var dest = walletRepository.getReferenceById(transactionDomain.walletDestinationId());
            entity.setDestinationWallet(dest);
        }

        if (transactionDomain.walletSourceId() != null) {
            var source = walletRepository.getReferenceById(transactionDomain.walletSourceId());
            entity.setSourceWallet(source);
        }

        return transactionEntityMapper.toDomain(transactionRepository.save(entity));
    }

    @Override
    public Optional<Wallet> findByUserId(UUID userId) throws ResourceNotFoundException {
        if (walletRepository.findByUserId(userId).isEmpty()) {
            throw new ResourceNotFoundException("Wallet not found with user id  :" + userId);
        }

        return Optional.ofNullable(walletEntityMapper.toDomain(walletRepository.findByUserId(userId).get()));
    }

    @Override
    public Long getBalance(UUID userId) {
        var balance = walletRepository.findBalanceByUserId(userId);

        // Si l'utilisateur n'a pas de wallet (ne devrait pas arriver), on renvoie 0
        return balance != null ? balance : 0L;
    }
}