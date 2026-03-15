package fr.uge.forkeat.infrastructure.persistence.postgres.projection;

import java.time.Instant;
import java.util.UUID;

/**
 * Projection Spring Data pour l'historique des super-likes avec jointures.
 */
public interface SuperLikeHistoryView {
    UUID getId();
    UUID getRecipeId();
    String getRecipeTitle();
    long getAmountCents();
    boolean getIsBonusFree();
    String getPromotionName();
    Instant getCreatedAt();
}
