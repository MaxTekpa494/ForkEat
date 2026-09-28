package fr.uge.forkeat.service.exception;

public class PromotionPriceException extends DomainException {
    public PromotionPriceException(long priceCents, long basePriceCents) {
        super("Le prix de la promotion (" + priceCents + " cts) doit être inférieur au prix de base (" + basePriceCents + " cts).");
    }
}
