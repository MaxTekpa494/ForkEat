package fr.uge.forkeat.service.exception;

public class CheckProfileUpdateFailureException extends RuntimeException{
  public CheckProfileUpdateFailureException(String message){
        super(message);
    }
}
