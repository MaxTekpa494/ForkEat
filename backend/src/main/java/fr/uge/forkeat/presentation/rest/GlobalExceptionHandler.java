package fr.uge.forkeat.presentation.rest;

import fr.uge.forkeat.service.exception.DuplicateTransactionException;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import fr.uge.forkeat.service.exception.StripEventException;
import fr.uge.forkeat.service.exception.WalletNotFoundException;

import java.time.Instant;
import java.util.Map;
import com.stripe.exception.SignatureVerificationException;

// À CHANGER POUR UTILISER DES HTTPRESPONSES PLUTOT QUE DES MAPS
@RestControllerAdvice(basePackages = "fr.uge.forkeat.presentation.rest")
public class GlobalExceptionHandler {

	@ExceptionHandler(RecipeNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleRecipeNotFound(RecipeNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Map.of("error", "Not Found", "message", e.getMessage(), "timestamp", Instant.now().toString() // On
																													// va
																													// ajouter
																													// plus
																													// de
																													// champs
																													// dans
																													// nos
																													// httpResponse
																													// les
																													// utiliser
																													// plutot
																													// que
																													// ça
				));
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

}