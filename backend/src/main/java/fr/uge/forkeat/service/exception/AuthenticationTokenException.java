package fr.uge.forkeat.service.exception;

public class AuthenticationTokenException extends RuntimeException{
    public AuthenticationTokenException(String message){
        super(message);
    }
}
