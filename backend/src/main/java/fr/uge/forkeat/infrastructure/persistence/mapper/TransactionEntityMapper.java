package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.TransactionEntity;
import fr.uge.forkeat.service.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.Objects;

public class TransactionEntityMapper {

    private TransactionEntityMapper() {}

    public static Transaction toDomain(TransactionEntity entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        return new Transaction(
                entity.getSourceWallet() != null ? entity.getSourceWallet().getId() : null,
                entity.getDestinationWallet() != null ? entity.getDestinationWallet().getId() : null,
                entity.getAmount(),
                entity.getTransactionType(),
                entity.getCreatedAt(),
                entity.getStripeTransactionID()
        );
    }

    public static TransactionEntity toEntity(Transaction domain) {
        Objects.requireNonNull(domain, "Domain cannot be null");

        TransactionEntity entity = new TransactionEntity();
        entity.setAmount(domain.amount());
        entity.setTransactionType(domain.type());
        entity.setCreatedAt(domain.createdAt());
        entity.setStripeTransactionID(domain.stripeTransactionID());

        return entity;
    }
}