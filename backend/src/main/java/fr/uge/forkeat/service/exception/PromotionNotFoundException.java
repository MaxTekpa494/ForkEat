package fr.uge.forkeat.service.exception;

import java.util.UUID;

public class PromotionNotFoundException extends RuntimeException {
    public PromotionNotFoundException(UUID id) {
        super("Promotion not found: " + id);
    }
}
