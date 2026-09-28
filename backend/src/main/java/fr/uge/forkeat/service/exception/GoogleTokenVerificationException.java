package fr.uge.forkeat.service.exception;

public class GoogleTokenVerificationException extends DomainException{

    protected GoogleTokenVerificationException(String message) {
        super(message);
    }

    public GoogleTokenVerificationException(String message, Throwable cause){
        super(message, cause);
    }
}
