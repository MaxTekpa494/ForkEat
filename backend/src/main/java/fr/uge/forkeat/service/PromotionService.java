package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.ForbiddenOperationException;
import fr.uge.forkeat.service.exception.PromotionModificationForbiddenException;
import fr.uge.forkeat.service.exception.PromotionNotFoundException;
import fr.uge.forkeat.service.exception.PromotionNotProfitableException;
import fr.uge.forkeat.service.exception.PromotionOverlapException;
import fr.uge.forkeat.service.model.superlike.Promotion;
import fr.uge.forkeat.service.model.superlike.PromotionStatus;
import fr.uge.forkeat.service.model.superlike.SuperLikeConfig;
import fr.uge.forkeat.service.model.superlike.SuperLikeHistory;
import fr.uge.forkeat.service.persistence.PromotionPersistence;
import fr.uge.forkeat.service.persistence.SuperLikeConfigPersistence;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PromotionService {

    private final PromotionPersistence promotionPersistence;
    private final SuperLikeConfigPersistence superLikeConfigPersistence;
    private final Logger logger = LoggerFactory.getLogger(PromotionService.class);
    private final AuthenticationPort authenticationPort;

    public PromotionService(PromotionPersistence promotionPersistence,
                            SuperLikeConfigPersistence superLikeConfigPersistence,
                            AuthenticationPort authenticationPort) {
        this.promotionPersistence = promotionPersistence;
        this.superLikeConfigPersistence = superLikeConfigPersistence;
        this.authenticationPort = authenticationPort;
    }

    public List<Promotion> findAll() {
        return promotionPersistence.findAll();
    }

    public Promotion findById(UUID id) {
        Objects.requireNonNull(id);
        return promotionPersistence.findById(id)
                .orElseThrow(() -> new PromotionNotFoundException(id));
    }

    public Optional<Promotion> findActive() {
        return promotionPersistence.findActiveAt(Instant.now());
    }

    public List<Promotion> findUpcoming() {
        return promotionPersistence.findScheduledOrActive().stream()
                .filter(p -> p.status() == PromotionStatus.SCHEDULED)
                .toList();
    }

    @Transactional
    public Promotion create(String name, Instant startsAt, Instant endsAt, long priceCents, Integer bonusEveryN) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(startsAt);
        Objects.requireNonNull(endsAt);

        if(!authenticationPort.isAdmin()){
            throw new ForbiddenOperationException("Only admins can create a promotion");
        }

        var config = superLikeConfigPersistence.get();
        validateProfitability(bonusEveryN, config);
        validateNoOverlap(startsAt, endsAt);

        var promotion = new Promotion(
                UUID.randomUUID(), name, startsAt, endsAt,
                priceCents, bonusEveryN, PromotionStatus.SCHEDULED, Instant.now()
        );
        var saved = promotionPersistence.save(promotion);
        logger.info("Promotion created: {} (starts={}, ends={})", saved.id(), startsAt, endsAt);
        return saved;
    }

    @Transactional
    public Promotion update(UUID id, String name, Instant startsAt, Instant endsAt, Long priceCents, Integer bonusEveryN) {
        Objects.requireNonNull(id);

        if(!authenticationPort.isAdmin()){
            throw new ForbiddenOperationException("Only admins can update a promotion");
        }

        var existing = promotionPersistence.findById(id)
                .orElseThrow(() -> new PromotionNotFoundException(id));

        if (!existing.isModifiable()) {
            throw new PromotionModificationForbiddenException(existing.status());
        }

        var config = superLikeConfigPersistence.get();
        var newBonusEveryN = bonusEveryN != null ? bonusEveryN : existing.bonusEveryN();
        validateProfitability(newBonusEveryN, config);

        var newStartsAt = startsAt != null ? startsAt : existing.startsAt();
        var newEndsAt   = endsAt   != null ? endsAt   : existing.endsAt();
        validateNoOverlap(newStartsAt, newEndsAt);

        var updated = new Promotion(
                id,
                name       != null ? name       : existing.name(),
                newStartsAt,
                newEndsAt,
                priceCents != null ? priceCents : existing.priceCents(),
                newBonusEveryN,
                existing.status(),
                existing.createdAt()
        );
        var saved = promotionPersistence.update(updated);
        logger.info("Promotion updated: {}", id);
        return saved;
    }

    @Transactional
    public void cancel(UUID id) {
        Objects.requireNonNull(id);

        if(!authenticationPort.isAdmin()){
            throw new ForbiddenOperationException("Only admins can cancel a promotion");
        }

        var existing = promotionPersistence.findById(id)
                .orElseThrow(() -> new PromotionNotFoundException(id));

        if (!existing.isModifiable()) {
            throw new PromotionModificationForbiddenException(existing.status());
        }
        promotionPersistence.transitionStatus(id, PromotionStatus.CANCELLED);
        logger.info("Promotion cancelled: {}", id);
    }

    public List<SuperLikeHistory> findSuperLikeHistory(UUID userId) {
        Objects.requireNonNull(userId);
        return promotionPersistence.findSuperLikeHistoryByUserId(userId);
    }

    public SuperLikeConfig getConfig() {
        return superLikeConfigPersistence.get();
    }

    @Transactional
    public SuperLikeConfig updateConfig(long priceCents, BigDecimal earningsRatio) {
        var updated = superLikeConfigPersistence.update(priceCents, earningsRatio);
        logger.info("SuperLike config updated: priceCents={}, earningsRatio={}", priceCents, earningsRatio);
        return updated;
    }

    // ─── Appelé par PromotionSchedulingService (tâches précises) ──────────

    @Transactional
    public void activate(UUID id) {
        var promo = promotionPersistence.findById(id)
                .orElseThrow(() -> new PromotionNotFoundException(id));
        if (promo.status() != PromotionStatus.SCHEDULED) {
            logger.warn("Promotion {} is not SCHEDULED (status={}), skipping activation", id, promo.status());
            return;
        }
        promotionPersistence.transitionStatus(id, PromotionStatus.ACTIVE);
        logger.info("Promotion {} activated", id);
    }

    @Transactional
    public void expire(UUID id) {
        var promo = promotionPersistence.findById(id)
                .orElseThrow(() -> new PromotionNotFoundException(id));
        if (promo.status() != PromotionStatus.ACTIVE) {
            logger.warn("Promotion {} is not ACTIVE (status={}), skipping expiry", id, promo.status());
            return;
        }
        promotionPersistence.transitionStatus(id, PromotionStatus.EXPIRED);
        logger.info("Promotion {} expired", id);
    }

    // ─── Réconciliation au démarrage (appelé par PromotionSchedulingService) ─

    @Transactional
    public List<Promotion> activateDuePromotions() {
        var now = Instant.now();
        var toActivate = promotionPersistence.findScheduledOrActive().stream()
                .filter(p -> p.status() == PromotionStatus.SCHEDULED && !p.startsAt().isAfter(now))
                .toList();

        for (var promo : toActivate) {
            promotionPersistence.transitionStatus(promo.id(), PromotionStatus.ACTIVE);
            logger.info("Promotion {} activated at startup (overdue)", promo.id());
        }
        return toActivate;
    }

    @Transactional
    public List<Promotion> expireDuePromotions() {
        var now = Instant.now();
        var toExpire = promotionPersistence.findScheduledOrActive().stream()
                .filter(p -> p.status() == PromotionStatus.ACTIVE
                        && p.endsAt() != null
                        && !p.endsAt().isAfter(now))
                .toList();

        for (var promo : toExpire) {
            promotionPersistence.transitionStatus(promo.id(), PromotionStatus.EXPIRED);
            logger.info("Promotion {} expired at startup (overdue)", promo.id());
        }
        return toExpire;
    }

    // ─── Validation ───────────────────────────────────────────────────────

    private void validateProfitability(Integer bonusEveryN, SuperLikeConfig config) {
        if (bonusEveryN == null) return;
        double ratio = config.earningsRatio().doubleValue();
        if (bonusEveryN * ratio <= (1 - ratio)) {
            throw new PromotionNotProfitableException(bonusEveryN, ratio);
        }
    }

    private void validateNoOverlap(Instant startsAt, Instant endsAt) {
        if (promotionPersistence.hasOverlapping(startsAt, endsAt)) {
            throw new PromotionOverlapException();
        }
    }
}
