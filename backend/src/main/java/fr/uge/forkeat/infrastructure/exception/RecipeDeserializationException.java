package fr.uge.forkeat.infrastructure.exception;

public class RecipeDeserializationException extends InfrastructureException{
  public RecipeDeserializationException(String message) {
    super(message);
  }

  public RecipeDeserializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
