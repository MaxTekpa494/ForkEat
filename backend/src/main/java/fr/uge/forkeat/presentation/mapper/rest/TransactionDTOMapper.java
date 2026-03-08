package fr.uge.forkeat.presentation.mapper.rest;

import fr.uge.forkeat.presentation.dto.user.TransactionDTO;
import fr.uge.forkeat.service.model.transaction.Transaction;

import java.util.Objects;

public class TransactionDTOMapper {

    private TransactionDTOMapper() {}

    public static TransactionDTO toDTO(Transaction transaction) {
        Objects.requireNonNull(transaction);
        return new TransactionDTO(
                transaction.id(),
                transaction.walletSourceId(),
                transaction.walletDestinationId(),
                transaction.amount(),
                transaction.type(),
                transaction.createdAt(),
                transaction.stripeTransactionID(),
                transaction.status()
        );
    }
}
