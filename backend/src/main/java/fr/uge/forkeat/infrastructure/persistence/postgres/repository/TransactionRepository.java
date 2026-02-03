package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.TransactionEntity;
import org.springframework.data.repository.CrudRepository;

public interface TransactionJpaRepository extends CrudRepository<TransactionEntity, String> {
    boolean existsByStripeTransactionID(String externalID);
}