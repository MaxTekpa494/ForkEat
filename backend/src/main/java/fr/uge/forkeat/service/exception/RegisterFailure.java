package fr.uge.forkeat.service.exception;

public class RegisterFailure extends RuntimeException{
  public RegisterFailure(String message){
    super(message);
  }
}
