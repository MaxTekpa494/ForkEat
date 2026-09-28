package fr.uge.forkeat.service.exception;

public class WithdrawalException extends DomainException {
    public WithdrawalException(String message) {
        super(message);
    }

    public WithdrawalException(String message, Throwable cause) {
        super(message, cause);
    }
}