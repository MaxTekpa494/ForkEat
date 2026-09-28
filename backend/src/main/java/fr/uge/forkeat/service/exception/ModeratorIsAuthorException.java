package fr.uge.forkeat.service.exception;

public class ModeratorIsAuthorException extends RuntimeException {
    public ModeratorIsAuthorException() {
        super("A moderator cannot moderate its own recipe or report");
    }
}

