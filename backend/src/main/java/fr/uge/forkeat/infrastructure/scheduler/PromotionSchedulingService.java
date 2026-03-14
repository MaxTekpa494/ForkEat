package fr.uge.forkeat.infrastructure.scheduler;

import fr.uge.forkeat.service.PromotionService;
import fr.uge.forkeat.service.model.superlike.Promotion;
import fr.uge.forkeat.service.port.PromotionSchedulingPort;
import fr.uge.forkeat.service.port.FcmGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Gère le scheduling précis des promotions via le TaskScheduler de Spring.
 * - Zéro latence : les tâches s'exécutent à l'instant exact de startsAt/endsAt.
 * - Résilient aux redémarrages : ApplicationRunner recharge depuis la BDD au boot.
 * - FCM appelé directement sans système d'événements intermédiaire.
 */
@Component
public class PromotionSchedulingService implements ApplicationRunner, PromotionSchedulingPort {

    private final PromotionService promotionService;
    private final FcmGateway fcmGateway;
    private final TaskScheduler taskScheduler;
    private final ConcurrentHashMap<UUID, List<ScheduledFuture<?>>> futures = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(PromotionSchedulingService.class);

    public PromotionSchedulingService(PromotionService promotionService,
                                      FcmGateway fcmGateway,
                                      TaskScheduler taskScheduler) {
        this.promotionService = promotionService;
        this.fcmGateway = fcmGateway;
        this.taskScheduler = taskScheduler;
    }

    public void onCreated(Promotion promotion) {
        schedule(promotion);
    }

    public void onUpdated(UUID oldId, Promotion promotion) {
        cancelFutures(oldId);
        schedule(promotion);
    }

    public void onCancelled(UUID id) {
        cancelFutures(id);
    }


    @Override
    public void run(ApplicationArguments args) {
        // Activer/expirer les promotions en retard (serveur était down)
        var activated = promotionService.activateDuePromotions();
        activated.forEach(p -> fcmGateway.sendToTopic("promotions", "Promotion en cours !", p.name()));

        var expired = promotionService.expireDuePromotions();
        expired.forEach(p -> fcmGateway.sendToTopic("promotions", "Fin de promotion", p.name()));

        // Planifier les promotions SCHEDULED futures
        promotionService.findUpcoming().forEach(this::schedule);

        // Planifier l'expiration de la promotion ACTIVE en cours
        promotionService.findActive().ifPresent(this::scheduleExpiryOnly);

        logger.info("PromotionSchedulingService: startup reconciliation done ({} activated, {} expired, {} scheduled)",
                activated.size(), expired.size(), promotionService.findUpcoming().size());
    }

    private void schedule(Promotion promo) {
        var scheduled = new ArrayList<ScheduledFuture<?>>();
        var now = Instant.now();

        if (promo.startsAt().isAfter(now)) {
            scheduled.add(taskScheduler.schedule(
                    () -> doActivate(promo.id(), promo.name()),
                    promo.startsAt()
            ));
        }

        if (promo.endsAt() != null && promo.endsAt().isAfter(now)) {
            scheduled.add(taskScheduler.schedule(
                    () -> doExpire(promo.id(), promo.name()),
                    promo.endsAt()
            ));
        }

        if (!scheduled.isEmpty()) {
            futures.put(promo.id(), scheduled);
            logger.info("Scheduled tasks for promotion {} (starts={}, ends={})",
                    promo.id(), promo.startsAt(), promo.endsAt());
        }
    }

    private void scheduleExpiryOnly(Promotion promo) {
        if (promo.endsAt() != null && promo.endsAt().isAfter(Instant.now())) {
            var future = taskScheduler.schedule(
                    () -> doExpire(promo.id(), promo.name()),
                    promo.endsAt()
            );
            futures.put(promo.id(), List.of(future));
        }
    }

    private void cancelFutures(UUID id) {
        var toCancel = futures.remove(id);
        if (toCancel != null) {
            toCancel.forEach(f -> f.cancel(false));
            logger.info("Cancelled scheduled tasks for promotion {}", id);
        }
    }

    private void doActivate(UUID id, String name) {
        try {
            promotionService.activate(id);
            fcmGateway.sendToTopic("promotions", "Promotion en cours !", name);
        } catch (Exception e) {
            logger.error("Failed to activate promotion {}: {}", id, e.getMessage(), e);
        }
    }

    private void doExpire(UUID id, String name) {
        try {
            promotionService.expire(id);
            fcmGateway.sendToTopic("promotions", "Fin de promotion", name);
        } catch (Exception e) {
            logger.error("Failed to expire promotion {}: {}", id, e.getMessage(), e);
        }
    }
}
