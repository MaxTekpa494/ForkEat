package fr.uge.forkeat.service.exception;

public class RegisterFailureException extends RuntimeException{
  public RegisterFailureException(String message){
    super(message);
  }
}
