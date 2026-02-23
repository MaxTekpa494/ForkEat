package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional; // Added import
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    boolean existsByStripeTransactionID(String externalID);

    @Query("SELECT t FROM TransactionEntity t WHERE t.sourceWallet.id = :walletId OR t.destinationWallet.id = :walletId ORDER BY t.createdAt DESC")
    List<TransactionEntity> findByWalletId(@Param("walletId") UUID walletId);
    Optional<TransactionEntity> findByStripeTransactionID(String externalID);
}