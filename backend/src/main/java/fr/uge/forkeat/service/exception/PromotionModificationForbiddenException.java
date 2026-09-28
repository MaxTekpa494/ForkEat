package fr.uge.forkeat.service.exception;

import fr.uge.forkeat.service.model.superlike.PromotionStatus;

public class PromotionModificationForbiddenException extends DomainException {
    public PromotionModificationForbiddenException(PromotionStatus currentStatus) {
        super("Cannot modify a promotion with status " + currentStatus + ". Only SCHEDULED promotions can be modified or cancelled.");
    }
}
