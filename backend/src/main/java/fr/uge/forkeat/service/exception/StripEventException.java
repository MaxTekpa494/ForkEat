package fr.uge.forkeat.service.exception;

public class StripEventException extends DomainException {

	public StripEventException(String message) {
		super(message);
	}

	public StripEventException(String message, Throwable cause) {
		super(message, cause);
	}

}
