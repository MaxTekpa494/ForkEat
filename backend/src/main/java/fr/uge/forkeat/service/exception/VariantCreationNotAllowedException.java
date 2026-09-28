package fr.uge.forkeat.service.exception;

import java.util.UUID;

public class VariantCreationNotAllowedException extends RuntimeException {

    public VariantCreationNotAllowedException(UUID parentId) {
        super("Cannot create a variant of recipe " + parentId);
    }
}