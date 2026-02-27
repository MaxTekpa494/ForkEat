package fr.uge.forkeat.presentation.rest;

import fr.uge.forkeat.service.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

// À CHANGER POUR UTILISER DES HTTPRESPONSES PLUTOT QUE DES MAPS
@RestControllerAdvice(basePackages = "fr.uge.forkeat.presentation")
public class GlobalRestExceptionHandler {

  @ExceptionHandler(RecipeNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleRecipeNotFound(RecipeNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Not Found", "message", e.getMessage(), "timestamp", Instant.now().toString() // On
            ));
		// va ajouter plus de champs dans
		// nos httpResponse les utiliser plutot que ça
  }


  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "Bad Request", "message", e.getMessage(), "timestamp", Instant.now().toString()));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Not Found", "message", e.getMessage(), "timestamp", Instant.now().toString()));
  }

  @ExceptionHandler(StripEventException.class)
  public ResponseEntity<Map<String, Object>> handleSignatureVerification(StripEventException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "Bad Request", "message", e.getMessage(), "timestamp", Instant.now().toString()));
  }

  @ExceptionHandler(DuplicateTransactionException.class)
  public ResponseEntity<Map<String, Object>> handleDuplicateTransaction(DuplicateTransactionException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", "Conflict", "message", e.getMessage(), "timestamp", Instant.now().toString()));
  }

  @ExceptionHandler(WalletNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleWalletNotFound(WalletNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Not Found", "message", e.getMessage(), "timestamp", Instant.now().toString()));
  }

  @ExceptionHandler(VerificationException.class)
  public ResponseEntity<Map<String, Object>> handleVerificationException(VerificationException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "Bad Request", "message", e.getMessage(), "timestamp", Instant.now().toString()));
  }

  @ExceptionHandler(RegisterFailureException.class)
  public ResponseEntity<Map<String, Object>> handleRegisterFailure(RegisterFailureException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "Bad Request", "message", e.getMessage(), "timestamp", Instant.now().toString()));
  }

  @ExceptionHandler(ImageUploadException.class)
  public ResponseEntity<Map<String, Object>> handleImageUploadException(ImageUploadException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "Bad Request", "message", e.getMessage(), "timestamp", Instant.now().toString()));
  }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, Object>> insufficientFundsException(InsufficientFundsException e) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(Map.of("error", "Bad Request", "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }

}

// S'inspirer pour corriger le controllerAdvance
/*
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - Ressource non trouvée
    @ExceptionHandler({RecipeNotFoundException.class, UserNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNotFound(RuntimeException ex) { }

    // 400 - Validation échouée
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(MethodArgumentNotValidException ex) { }

    // 409 - Conflit (contrainte unique)
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleConflict(DataIntegrityViolationException ex) { }

    // 401 - Non authentifié
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleUnauthorized(UnauthorizedException ex) { }

    // 403 - Interdit (pas les droits)
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleForbidden(AccessDeniedException ex) { }

    // 500 - Erreur serveur générique
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGeneric(Exception ex) { }
}
 */