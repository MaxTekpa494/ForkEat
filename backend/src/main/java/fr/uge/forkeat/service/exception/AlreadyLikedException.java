package fr.uge.forkeat.service.exception;

public class AlreadyLikedException extends RuntimeException{
    public AlreadyLikedException(String message){
        super(message);
    }
}
