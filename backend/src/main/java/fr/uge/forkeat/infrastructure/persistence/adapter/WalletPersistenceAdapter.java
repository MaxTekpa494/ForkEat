package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.mapper.TransactionMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.mapper.WalletMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.TransactionJpaRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserJpaRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.WalletJpaRepository;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.WalletNotFoundException;
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
    private final UserJpaRepository userRepository;

    // LES MAPPERS
    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;

    public WalletPersistenceAdapter(WalletJpaRepository walletRepository,
                                    TransactionJpaRepository transactionRepository,
                                    UserJpaRepository userRepository,
                                    WalletMapper walletMapper,
                                    TransactionMapper transactionMapper) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
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
    public Wallet saveWallet(Wallet wallet) {
        var entity = walletRepository.findById(wallet.id())
                .orElseGet(() -> {
                    WalletEntity newEntity = new WalletEntity();
                    newEntity.setId(wallet.id());

                    var userEntity = userRepository.findById(wallet.userId())
                            .orElseThrow(() -> new IllegalStateException("User not found: " + wallet.userId()));
                    userEntity.setWallet(newEntity);

                    return newEntity;
                });

        walletMapper.updateEntity(entity, wallet);
        var saved = walletRepository.save(entity);
        return walletMapper.toDomain(saved);
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
    public Optional<Wallet> findByUserId(UUID userId) throws ResourceNotFoundException {
        if(walletRepository.findByUserId(userId).isEmpty()){
            throw new ResourceNotFoundException("Wallet not found with user id  :" + userId);
        }

        return Optional.ofNullable(walletMapper.toDomain(walletRepository.findByUserId(userId).get()));
    }

    @Override
    public Long getBalance(UUID userId) {
        var balance = walletRepository.findBalanceByUserId(userId);

        // Si l'utilisateur n'a pas de wallet (ne devrait pas arriver), on renvoie 0
        return balance != null ? balance : 0L;
    }
}