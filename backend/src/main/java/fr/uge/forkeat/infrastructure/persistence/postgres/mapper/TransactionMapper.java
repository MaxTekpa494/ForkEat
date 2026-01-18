package fr.uge.forkeat.infrastructure.persistence.postgres.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.TransactionEntity;
import fr.uge.forkeat.service.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public Transaction toDomain(TransactionEntity entity) {
        if (entity == null) return null;

        return new Transaction(
                entity.getWalletSource() != null ? entity.getWalletSource().getId() : null,
                entity.getWalletDestination() != null ? entity.getWalletDestination().getId() : null,
                entity.getAmount(),
                entity.getType(),
                entity.getCreatedAt(),
                entity.getStripeTransactionId()
        );
    }

    public TransactionEntity toEntity(Transaction domain) {
        if (domain == null) return null;

        TransactionEntity entity = new TransactionEntity();
        entity.setAmount(domain.amount());
        entity.setType(domain.type());
        entity.setCreatedAt(domain.createdAt());
        entity.setStripeTransactionId(domain.stripeTransactionId());

        return entity;
    }
}