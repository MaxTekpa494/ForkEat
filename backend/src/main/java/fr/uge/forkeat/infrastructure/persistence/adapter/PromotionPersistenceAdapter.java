package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.PromotionEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.PromotionRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.SuperLikeRepository;
import fr.uge.forkeat.service.exception.PromotionNotFoundException;
import fr.uge.forkeat.service.model.superlike.Promotion;
import fr.uge.forkeat.service.model.superlike.PromotionStatus;
import fr.uge.forkeat.service.model.superlike.SuperLikeHistory;
import fr.uge.forkeat.service.persistence.PromotionPersistence;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PromotionPersistenceAdapter implements PromotionPersistence {

    private final PromotionRepository promotionRepository;
    private final SuperLikeRepository superLikeRepository;

    public PromotionPersistenceAdapter(PromotionRepository promotionRepository,
                                       SuperLikeRepository superLikeRepository) {
        this.promotionRepository = promotionRepository;
        this.superLikeRepository = superLikeRepository;
    }

    @Override
    public Promotion save(Promotion promotion) {
        var entity = PromotionEntityMapper.toEntity(promotion);
        return PromotionEntityMapper.toDomain(promotionRepository.save(entity));
    }

    @Override
    public Optional<Promotion> findById(UUID id) {
        return promotionRepository.findById(id).map(PromotionEntityMapper::toDomain);
    }

    @Override
    public List<Promotion> findAll() {
        return promotionRepository.findAll().stream()
                .map(PromotionEntityMapper::toDomain)
                .toList();
    }

    @Override
    public Promotion update(Promotion promotion) {
        var entity = promotionRepository.findById(promotion.id())
                .orElseThrow(() -> new PromotionNotFoundException(promotion.id()));
        entity.setName(promotion.name());
        entity.setStartsAt(promotion.startsAt());
        entity.setEndsAt(promotion.endsAt());
        entity.setPriceCents(promotion.priceCents());
        entity.setBonusEveryN(promotion.bonusEveryN());
        entity.setStatus(promotion.status());
        return PromotionEntityMapper.toDomain(promotionRepository.save(entity));
    }

    @Override
    public Optional<Promotion> findActiveAt(Instant instant) {
        return promotionRepository.findActiveAt(instant).map(PromotionEntityMapper::toDomain);
    }

    @Override
    public List<Promotion> findScheduledOrActive() {
        return promotionRepository.findScheduledOrActive().stream()
                .map(PromotionEntityMapper::toDomain)
                .toList();
    }

    @Override
    public boolean hasOverlapping(Instant startsAt, Instant endsAt, UUID excludeId) {
        if (excludeId == null) {
            return promotionRepository.hasOverlappingAny(startsAt, endsAt);
        }
        return promotionRepository.hasOverlappingExcluding(startsAt, endsAt, excludeId.toString());
    }

    @Override
    public int countPaidSuperLikesByUserAndPromotion(UUID userId, UUID promotionId) {
        return superLikeRepository.countPaidByUserAndPromotion(userId, promotionId);
    }

    @Override
    public void transitionStatus(UUID id, PromotionStatus newStatus) {
        promotionRepository.updateStatus(id, newStatus);
    }

    @Override
    public List<SuperLikeHistory> findSuperLikeHistoryByUserId(UUID userId) {
        return superLikeRepository.findHistoryByUserId(userId).stream()
                .map(v -> new SuperLikeHistory(
                        v.getId(),
                        v.getRecipeId(),
                        v.getRecipeTitle(),
                        v.getAmountCents(),
                        v.getIsBonusFree(),
                        v.getPromotionName(),
                        v.getCreatedAt()
                ))
                .toList();
    }
}
