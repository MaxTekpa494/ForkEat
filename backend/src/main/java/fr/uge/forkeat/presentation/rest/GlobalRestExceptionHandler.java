package fr.uge.forkeat.presentation.rest;

import fr.uge.forkeat.service.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

// À CHANGER POUR UTILISER DES HTTPRESPONSES PLUTOT QUE DES MAPS
@RestControllerAdvice(basePackages = {"fr.uge.forkeat.presentation.rest", "fr.uge.forkeat.presentation.external"})
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

    @ExceptionHandler(CheckProfileUpdateFailureException.class)
    public ResponseEntity<Map<String, String>> handleCheckProfileUpdateFailure(CheckProfileUpdateFailureException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Bad Request", "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(ModerationRagException.class)
    public ResponseEntity<Map<String, Object>> handleModeration(ModerationRagException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Bad Request",
                        "message", e.getMessage(),
                        "timestamp", Instant.now().toString()
                ));
    }

    @ExceptionHandler(AuthenticationTokenException.class)
    public ResponseEntity<Map<String, Object>> authenticationException(AuthenticationTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Unauthorized", "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }


    @ExceptionHandler(RecipeAlreadyReportedException.class)
    public ResponseEntity<Map<String, String>> handleRecipeAlreadyReported(RecipeAlreadyReportedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Conflict", "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(UserAlreadyReportedException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyReported(UserAlreadyReportedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Conflict", "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(PromotionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePromotionNotFound(PromotionNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Not Found", "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(PromotionOverlapException.class)
    public ResponseEntity<Map<String, Object>> handlePromotionOverlap(PromotionOverlapException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Conflict", "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(PromotionModificationForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handlePromotionModificationForbidden(PromotionModificationForbiddenException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "Unprocessable Entity", "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(PromotionNotProfitableException.class)
    public ResponseEntity<Map<String, Object>> handlePromotionNotProfitable(PromotionNotProfitableException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "Unprocessable Entity", "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }

}