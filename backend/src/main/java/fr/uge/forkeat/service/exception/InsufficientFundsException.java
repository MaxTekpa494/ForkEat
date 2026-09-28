package fr.uge.forkeat.service.exception;

public class InsufficientFundsException extends DomainException {
    private final Long available;
    private final Long required;

    public InsufficientFundsException(Long available, Long required) {
        super(String.format("Insufficient funds: available %s, required %s",
                available, required));
        this.available = available;
        this.required = required;
    }

    public Long getAvailable() { return available; }
    public Long getRequired() { return required; }
}
