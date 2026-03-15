package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.TransactionEntity;
import fr.uge.forkeat.service.model.transaction.Transaction;
import fr.uge.forkeat.service.model.transaction.TransactionType;
import fr.uge.forkeat.service.model.transaction.TransactionStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TransactionEntityMapperTest {


    @Test
    void transactionEntityToTransaction() {
        var entity = new TransactionEntity(
                null,
                null,
                1000L,
                "stripe_123",
                TransactionType.RECHARGE,
                TransactionStatus.SUCCEEDED,
                Instant.now()
        );

        var transaction = TransactionEntityMapper.toDomain(entity);

        assertNotNull(transaction);
        assertEquals(entity.getStripeTransactionID(), transaction.stripeTransactionID());
        assertEquals(entity.getTransactionType(), transaction.type());
        assertEquals(entity.getStatus(), transaction.status());
    }

    @Test
    void transactionToTransactionEntity() {
        var transaction = new Transaction(
                UUID.randomUUID(),
                null,
                null,
                1000L,
                TransactionType.RECHARGE,
                Instant.now(),
                "stripe_123",
                TransactionStatus.SUCCEEDED
        );

        var entity = TransactionEntityMapper.toEntity(transaction);

        assertNotNull(entity);
        assertEquals(entity.getStripeTransactionID(), transaction.stripeTransactionID());
        assertEquals(entity.getTransactionType(), transaction.type());
        assertEquals(entity.getStatus(), transaction.status());
    }
}
