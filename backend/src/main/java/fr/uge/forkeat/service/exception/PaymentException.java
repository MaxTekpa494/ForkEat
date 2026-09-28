package fr.uge.forkeat.service.exception;

public class PaymentException extends DomainException{

    public PaymentException(String message){
        super(message);
    }

    public PaymentException(String message, Throwable cause){
        super(message, cause);
    }
}