package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.TransactionEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.TransactionEntity;
import fr.uge.forkeat.infrastructure.persistence.mapper.WalletEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.TransactionRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.WalletRepository;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.Transaction;
import fr.uge.forkeat.service.model.user.Wallet;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class WalletPersistenceAdapter implements WalletPersistence {

	private final WalletRepository walletRepository;
	private final TransactionRepository transactionRepository;
	private final UserRepository userRepository;

	public WalletPersistenceAdapter(WalletRepository walletRepository,
			TransactionRepository transactionRepository, UserRepository userRepository) {
		this.walletRepository = Objects.requireNonNull(walletRepository);
		this.transactionRepository = Objects.requireNonNull(transactionRepository);
		this.userRepository = Objects.requireNonNull(userRepository);
	}

	@Override
	public Optional<Wallet> loadWalletWithLock(UUID userId) {
		return walletRepository.findByUserId(userId).map(WalletEntityMapper::toDomain);
	}

	@Override
	public Optional<Wallet> getWalletById(UUID walletId) {
		return walletRepository.findById(walletId).map(WalletEntityMapper::toDomain);
	}

	@Override
	public Wallet saveWallet(Wallet wallet) {
		var entity = walletRepository.findById(wallet.id()).orElseGet(() -> {
			WalletEntity newEntity = new WalletEntity();
			newEntity.setId(wallet.id());

			var userEntity = userRepository.findById(wallet.userId())
					.orElseThrow(() -> new IllegalStateException("User not found: " + wallet.userId()));
			newEntity.setUser(userEntity);

			return newEntity;
		});

		entity.setBalance(wallet.balance());
		entity.setUpdatedAt(wallet.updatedAt());
		var saved = walletRepository.save(entity);
		return WalletEntityMapper.toDomain(saved);
	}

	@Override
	public boolean transactionExists(String externalId) {
		return transactionRepository.existsByStripeTransactionID(externalId);
	}

	@Override
	public Transaction saveTransaction(Transaction transactionDomain) {
		var entity = TransactionEntityMapper.toEntity(transactionDomain);

		if (transactionDomain.walletDestinationId() != null) {
			var dest = walletRepository.getReferenceById(transactionDomain.walletDestinationId());
			entity.setDestinationWallet(dest);
		}

		if (transactionDomain.walletSourceId() != null) {
			var source = walletRepository.getReferenceById(transactionDomain.walletSourceId());
			entity.setSourceWallet(source);
		}

		return TransactionEntityMapper.toDomain(transactionRepository.save(entity));
	}

	@Override
	public Optional<Wallet> findByUserId(UUID userId) {
		return walletRepository.findByUserIdReadOnly(userId).map(WalletEntityMapper::toDomain);
	}

	@Override
	public long getBalance(UUID userId) {
		var balance = walletRepository.findBalanceByUserId(userId);

		// Si l'utilisateur n'a pas de wallet (ne devrait pas arriver), on renvoie 0
		return balance != null ? balance : 0L;
	}

	@Override
	public List<Transaction> getTransactionsByUserId(UUID userId) {
		var wallet = walletRepository.findByUserIdReadOnly(userId);
		if (wallet.isEmpty()) {
			return List.of();
		}
		return transactionRepository.findByWalletId(wallet.get().getId())
				.stream()
				.map(TransactionEntityMapper::toDomain)
				.toList();
	}

    @Override
    public Optional<Transaction> findTransactionByStripeTransactionID(String stripeTransactionID) {
        Objects.requireNonNull(stripeTransactionID);
        return transactionRepository.findByStripeTransactionID(stripeTransactionID)
                .map(TransactionEntityMapper::toDomain);
    }

    @Override
    public Optional<Transaction> findTransactionById(UUID id) {
        Objects.requireNonNull(id);
        return transactionRepository.findById(id).map(TransactionEntityMapper::toDomain);
    }

    @Override
    public Transaction updateTransaction(Transaction transaction) {
        Objects.requireNonNull(transaction);

        TransactionEntity existingEntity = transactionRepository.findById(transaction.id())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transaction.id()));

        // Update fields that can change
        existingEntity.setStripeTransactionID(transaction.stripeTransactionID());
        existingEntity.setStatus(transaction.status());

        if (transaction.walletDestinationId() != null) {
            existingEntity.setDestinationWallet(walletRepository.getReferenceById(transaction.walletDestinationId()));
        } else {
            existingEntity.setDestinationWallet(null);
        }

        if (transaction.walletSourceId() != null) {
            existingEntity.setSourceWallet(walletRepository.getReferenceById(transaction.walletSourceId()));
        } else {
            existingEntity.setSourceWallet(null);
        }

        return TransactionEntityMapper.toDomain(transactionRepository.save(existingEntity));
    }
}
