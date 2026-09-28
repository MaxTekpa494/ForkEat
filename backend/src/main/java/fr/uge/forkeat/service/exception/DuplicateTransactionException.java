package fr.uge.forkeat.service.exception;

public class DuplicateTransactionException extends DomainException {
    public DuplicateTransactionException(String transactionId) {
        super("Transaction already processed: " + transactionId);
    }
}