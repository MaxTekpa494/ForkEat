package fr.uge.forkeat.presentation.rest;

import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecipeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRecipeNotFound(RecipeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Not Found",
                "message", e.getMessage(),
                "timestamp", Instant.now().toString() // On va ajouter plus de champs dans nos httpResponse les utiliser plutot que ça
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Bad Request",
                "message", e.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }
}
