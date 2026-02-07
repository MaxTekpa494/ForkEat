package fr.uge.forkeat.service.exception;

public class CheckProfileUpdateFailure extends RuntimeException{
  public CheckProfileUpdateFailure(String message){
        super(message);
    }
}
