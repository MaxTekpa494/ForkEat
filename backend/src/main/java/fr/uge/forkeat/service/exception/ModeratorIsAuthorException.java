package fr.uge.forkeat.service.exception;

public class ModeratorIsAuthorException extends RuntimeException {
    public ModeratorIsAuthorException() {
        super("Un modérateur ne peut pas modérer sa propre recette.");
    }
}

