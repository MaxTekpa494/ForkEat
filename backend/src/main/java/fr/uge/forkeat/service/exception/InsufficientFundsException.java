package fr.uge.forkeat.service.exception;

public class InsufficientFundsException extends RuntimeException{

    public InsufficientFundsException(String message){
        super(message);
    }

    public InsufficientFundsException(){}
}
