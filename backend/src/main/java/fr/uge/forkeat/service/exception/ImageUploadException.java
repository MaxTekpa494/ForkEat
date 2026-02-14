package fr.uge.forkeat.service.exception;

// Cette exception est utilisée dans l'infra mais c'est très important
public class ImageUploadException extends RuntimeException {


  public ImageUploadException(String message) {
    super(message);
  }

  public ImageUploadException(String message, Throwable cause) {
    super(message, cause);
  }
}