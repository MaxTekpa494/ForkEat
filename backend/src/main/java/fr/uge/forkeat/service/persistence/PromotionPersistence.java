package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.superlike.Promotion;
import fr.uge.forkeat.service.model.superlike.PromotionStatus;
import fr.uge.forkeat.service.model.superlike.SuperLikeHistory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionPersistence {
    Promotion save(Promotion promotion);
    Optional<Promotion> findById(UUID id);
    List<Promotion> findAll();
    Promotion update(Promotion promotion);
    Optional<Promotion> findActiveAt(Instant instant);
    List<Promotion> findScheduledOrActive();
    boolean hasOverlapping(Instant startsAt, Instant endsAt);
    int countPaidSuperLikesByUserAndPromotion(UUID userId, UUID promotionId);
    void transitionStatus(UUID id, PromotionStatus newStatus);
    List<SuperLikeHistory> findSuperLikeHistoryByUserId(UUID userId);
}
