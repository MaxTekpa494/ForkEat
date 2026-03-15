package fr.uge.forkeat.service.exception;

public class PromotionOverlapException extends DomainException {
    public PromotionOverlapException() {
        super("A promotion already exists for this time period. Two promotions cannot be active simultaneously.");
    }
}
