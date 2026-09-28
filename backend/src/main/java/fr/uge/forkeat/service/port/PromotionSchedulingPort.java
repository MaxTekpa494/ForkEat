package fr.uge.forkeat.service.port;

import fr.uge.forkeat.service.model.superlike.Promotion;

import java.util.UUID;

public interface PromotionSchedulingPort {
    void onCreated(Promotion promotion);
    void onUpdated(UUID oldId, Promotion promotion);
    void onCancelled(UUID id);
}
