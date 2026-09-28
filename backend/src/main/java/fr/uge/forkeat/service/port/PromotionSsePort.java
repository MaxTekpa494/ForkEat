package fr.uge.forkeat.service.port;

import fr.uge.forkeat.service.model.superlike.Promotion;

public interface PromotionSsePort {
    void notifyActivated(Promotion promotion);
    void notifyExpired(Promotion promotion);
}
