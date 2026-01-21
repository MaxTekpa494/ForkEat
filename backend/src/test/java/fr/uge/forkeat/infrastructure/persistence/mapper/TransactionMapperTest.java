package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.TransactionEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.mapper.TransactionMapper;
import fr.uge.forkeat.service.model.Transaction;
import fr.uge.forkeat.service.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionMapperTest {

    private final TransactionMapper mapper = new TransactionMapper();

    @Test
    void transactionEntityToTransaction() {
        var entity = new TransactionEntity(null,
                null,
                1000L,
                "stripe_123",
                TransactionType.RECHARGE,
                Instant.now()
        );

        var transaction = mapper.toDomain(entity);

        assertEquals(entity.getStripeTransactionID(), transaction.stripeTransactionID());
        assertEquals(entity.getTransactionType(), transaction.type());
    }

    @Test
    void transactionToTransactionEntity() {
        var transaction = new Transaction(null,
                null,
                1000L,
                TransactionType.RECHARGE,
                Instant.now(),
                "stripe_123"
                );

        var entity = mapper.toEntity(transaction);

        assertEquals(entity.getStripeTransactionID(), transaction.stripeTransactionID());
        assertEquals(entity.getTransactionType(), transaction.type());
    }
}