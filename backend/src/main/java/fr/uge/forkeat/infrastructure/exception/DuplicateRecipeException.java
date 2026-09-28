package fr.uge.forkeat.infrastructure.exception;

public class DuplicateRecipeException extends InfrastructureException{
  public DuplicateRecipeException(String message) {
    super(message);
  }

  public DuplicateRecipeException(String message, Throwable cause) {
    super(message, cause);
  }
}
