package fr.uge.forkeat.service.exception;

public class ModerationRagException extends DomainException{
  public ModerationRagException(String raison){
        super(raison);
  }
}
