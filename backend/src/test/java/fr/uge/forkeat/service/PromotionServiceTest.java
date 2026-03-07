package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.PromotionModificationForbiddenException;
import fr.uge.forkeat.service.exception.PromotionNotFoundException;
import fr.uge.forkeat.service.exception.PromotionNotProfitableException;
import fr.uge.forkeat.service.exception.PromotionOverlapException;
import fr.uge.forkeat.service.model.superlike.Promotion;
import fr.uge.forkeat.service.model.superlike.PromotionStatus;
import fr.uge.forkeat.service.model.superlike.SuperLikeConfig;
import fr.uge.forkeat.service.persistence.PromotionPersistence;
import fr.uge.forkeat.service.persistence.SuperLikeConfigPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionPersistence promotionPersistence;
    @Mock
    private SuperLikeConfigPersistence superLikeConfigPersistence;

    private PromotionService promotionService;
    private SuperLikeConfig defaultConfig;

    @BeforeEach
    void setUp() {
        promotionService = new PromotionService(promotionPersistence, superLikeConfigPersistence);
        defaultConfig = new SuperLikeConfig(UUID.randomUUID(), 100L, new BigDecimal("0.40"), Instant.now());
    }

    /** Crée une promo SCHEDULED avec startsAt dans le futur. */
    private Promotion scheduled(UUID id) {
        var now = Instant.now();
        return new Promotion(id, "Promo", now.plusSeconds(60), now.plusSeconds(3660), 100L, null, PromotionStatus.SCHEDULED, now);
    }

    // ─── FindById ──────────────────────────────────────────────────────────

    @Nested
    class FindById {

        @Test
        void shouldReturnPromotion_whenFound() {
            var id = UUID.randomUUID();
            var promo = scheduled(id);
            when(promotionPersistence.findById(id)).thenReturn(Optional.of(promo));

            var result = promotionService.findById(id);

            assertEquals(id, result.id());
            assertEquals("Promo", result.name());
            verify(promotionPersistence).findById(id);
        }

        @Test
        void shouldThrow_whenNotFound() {
            var id = UUID.randomUUID();
            when(promotionPersistence.findById(id)).thenReturn(Optional.empty());

            assertThrows(PromotionNotFoundException.class, () -> promotionService.findById(id));
        }

        @Test
        void shouldThrowNPE_whenIdIsNull() {
            assertThrows(NullPointerException.class, () -> promotionService.findById(null));
        }
    }

    // ─── FindActive ────────────────────────────────────────────────────────

    @Nested
    class FindActive {

        @Test
        void shouldDelegateToPersistenceAndReturnResult() {
            var id = UUID.randomUUID();
            var now = Instant.now();
            var active = new Promotion(id, "Promo", now.minusSeconds(60), now.plusSeconds(3600), 100L, null, PromotionStatus.ACTIVE, now);
            when(promotionPersistence.findActiveAt(any())).thenReturn(Optional.of(active));

            var result = promotionService.findActive();

            assertTrue(result.isPresent());
            assertEquals(id, result.get().id());
        }

        @Test
        void shouldReturnEmpty_whenNoActivePromotion() {
            when(promotionPersistence.findActiveAt(any())).thenReturn(Optional.empty());

            assertTrue(promotionService.findActive().isEmpty());
        }
    }

    // ─── FindUpcoming ──────────────────────────────────────────────────────

    @Nested
    class FindUpcoming {

        @Test
        void shouldReturnOnlyScheduledPromotions() {
            var now = Instant.now();
            var sched = new Promotion(UUID.randomUUID(), "Sched", now.plusSeconds(60), now.plusSeconds(3660), 100L, null, PromotionStatus.SCHEDULED, now);
            var active = new Promotion(UUID.randomUUID(), "Active", now.minusSeconds(60), now.plusSeconds(3600), 100L, null, PromotionStatus.ACTIVE, now);
            when(promotionPersistence.findScheduledOrActive()).thenReturn(List.of(sched, active));

            var result = promotionService.findUpcoming();

            assertEquals(1, result.size());
            assertEquals(PromotionStatus.SCHEDULED, result.getFirst().status());
        }

        @Test
        void shouldReturnEmptyList_whenNoneScheduled() {
            when(promotionPersistence.findScheduledOrActive()).thenReturn(List.of());

            assertTrue(promotionService.findUpcoming().isEmpty());
        }
    }

    // ─── Create ────────────────────────────────────────────────────────────

    @Nested
    class Create {

        @Test
        void shouldSavePromotion() {
            var now = Instant.now();
            var starts = now.plusSeconds(60);
            var ends = now.plusSeconds(3660);
            var saved = new Promotion(UUID.randomUUID(), "Promo", starts, ends, 100L, null, PromotionStatus.SCHEDULED, now);

            when(superLikeConfigPersistence.get()).thenReturn(defaultConfig);
            when(promotionPersistence.hasOverlapping(starts, ends, null)).thenReturn(false);
            when(promotionPersistence.save(any())).thenReturn(saved);

            var result = promotionService.create("Promo", starts, ends, 100L, null);

            assertNotNull(result);
            assertEquals("Promo", result.name());
            verify(promotionPersistence).save(any());
        }

        @Test
        void shouldThrowNPE_whenNameIsNull() {
            var now = Instant.now();
            assertThrows(NullPointerException.class,
                    () -> promotionService.create(null, now.plusSeconds(60), now.plusSeconds(3660), 100L, null));
        }

        @Test
        void shouldThrowNPE_whenStartsAtIsNull() {
            var now = Instant.now();
            assertThrows(NullPointerException.class,
                    () -> promotionService.create("Promo", null, now.plusSeconds(3660), 100L, null));
        }

        @Test
        void shouldThrowNPE_whenEndsAtIsNull() {
            var now = Instant.now();
            assertThrows(NullPointerException.class,
                    () -> promotionService.create("Promo", now.plusSeconds(60), null, 100L, null));
        }

        @Test
        void shouldThrowOverlapException_whenOverlapExists() {
            var now = Instant.now();
            var starts = now.plusSeconds(60);
            var ends = now.plusSeconds(3660);

            when(superLikeConfigPersistence.get()).thenReturn(defaultConfig);
            when(promotionPersistence.hasOverlapping(starts, ends, null)).thenReturn(true);

            assertThrows(PromotionOverlapException.class,
                    () -> promotionService.create("Promo", starts, ends, 100L, null));
            verify(promotionPersistence, never()).save(any());
        }

        @Test
        void shouldThrowNotProfitable_whenBonusEveryNTooLow() {
            // ratio=0.10 → min bonusEveryN = floor(0.90/0.10)+1 = 10, bonusEveryN=5 fails
            var config = new SuperLikeConfig(UUID.randomUUID(), 100L, new BigDecimal("0.10"), Instant.now());
            when(superLikeConfigPersistence.get()).thenReturn(config);

            var now = Instant.now();
            assertThrows(PromotionNotProfitableException.class,
                    () -> promotionService.create("Promo", now.plusSeconds(60), now.plusSeconds(3660), 100L, 5));
            verify(promotionPersistence, never()).save(any());
        }
    }

    // ─── Update ────────────────────────────────────────────────────────────

    @Nested
    class Update {

        @Test
        void shouldUpdateScheduledPromotion() {
            var id = UUID.randomUUID();
            var existing = scheduled(id);
            var updated = new Promotion(id, "Updated", existing.startsAt(), existing.endsAt(), 200L, null, PromotionStatus.SCHEDULED, existing.createdAt());

            when(promotionPersistence.findById(id)).thenReturn(Optional.of(existing));
            when(superLikeConfigPersistence.get()).thenReturn(defaultConfig);
            when(promotionPersistence.hasOverlapping(any(), any(), eq(id))).thenReturn(false);
            when(promotionPersistence.update(any())).thenReturn(updated);

            var result = promotionService.update(id, "Updated", null, null, 200L, null);

            assertEquals("Updated", result.name());
            assertEquals(200L, result.priceCents());
            verify(promotionPersistence).update(any());
        }

        @Test
        void shouldThrow_whenNotFound() {
            var id = UUID.randomUUID();
            when(promotionPersistence.findById(id)).thenReturn(Optional.empty());

            assertThrows(PromotionNotFoundException.class,
                    () -> promotionService.update(id, "X", null, null, null, null));
        }

        @Test
        void shouldThrowModificationForbidden_whenActive() {
            var id = UUID.randomUUID();
            var now = Instant.now();
            var active = new Promotion(id, "Promo", now.minusSeconds(60), now.plusSeconds(3600), 100L, null, PromotionStatus.ACTIVE, now);
            when(promotionPersistence.findById(id)).thenReturn(Optional.of(active));

            assertThrows(PromotionModificationForbiddenException.class,
                    () -> promotionService.update(id, null, null, null, null, null));
            verify(promotionPersistence, never()).update(any());
        }

        @Test
        void shouldThrowModificationForbidden_whenExpired() {
            var id = UUID.randomUUID();
            var now = Instant.now();
            var expired = new Promotion(id, "Promo", now.minusSeconds(7200), now.minusSeconds(3600), 100L, null, PromotionStatus.EXPIRED, now);
            when(promotionPersistence.findById(id)).thenReturn(Optional.of(expired));

            assertThrows(PromotionModificationForbiddenException.class,
                    () -> promotionService.update(id, null, null, null, null, null));
            verify(promotionPersistence, never()).update(any());
        }

        @Test
        void shouldPreserveExistingValues_whenNullsProvided() {
            var id = UUID.randomUUID();
            var existing = scheduled(id);
            var preserved = new Promotion(id, existing.name(), existing.startsAt(), existing.endsAt(), existing.priceCents(), null, PromotionStatus.SCHEDULED, existing.createdAt());

            when(promotionPersistence.findById(id)).thenReturn(Optional.of(existing));
            when(superLikeConfigPersistence.get()).thenReturn(defaultConfig);
            when(promotionPersistence.hasOverlapping(any(), any(), eq(id))).thenReturn(false);
            when(promotionPersistence.update(any())).thenReturn(preserved);

            var result = promotionService.update(id, null, null, null, null, null);

            assertEquals(existing.name(), result.name());
            assertEquals(existing.priceCents(), result.priceCents());
        }
    }

    // ─── Cancel ────────────────────────────────────────────────────────────

    @Nested
    class Cancel {

        @Test
        void shouldCancelScheduledPromotion() {
            var id = UUID.randomUUID();
            when(promotionPersistence.findById(id)).thenReturn(Optional.of(scheduled(id)));

            promotionService.cancel(id);

            verify(promotionPersistence).transitionStatus(id, PromotionStatus.CANCELLED);
        }

        @Test
        void shouldThrow_whenNotFound() {
            var id = UUID.randomUUID();
            when(promotionPersistence.findById(id)).thenReturn(Optional.empty());

            assertThrows(PromotionNotFoundException.class, () -> promotionService.cancel(id));
            verify(promotionPersistence, never()).transitionStatus(any(), any());
        }

        @Test
        void shouldThrowModificationForbidden_whenActive() {
            var id = UUID.randomUUID();
            var now = Instant.now();
            var active = new Promotion(id, "Promo", now.minusSeconds(60), now.plusSeconds(3600), 100L, null, PromotionStatus.ACTIVE, now);
            when(promotionPersistence.findById(id)).thenReturn(Optional.of(active));

            assertThrows(PromotionModificationForbiddenException.class, () -> promotionService.cancel(id));
            verify(promotionPersistence, never()).transitionStatus(any(), any());
        }

        @Test
        void shouldThrowNPE_whenIdIsNull() {
            assertThrows(NullPointerException.class, () -> promotionService.cancel(null));
        }
    }

    // ─── ActivateDuePromotions ─────────────────────────────────────────────

    @Nested
    class ActivateDuePromotions {

        @Test
        void shouldActivatePromo_whenStartsAtIsInThePast() {
            var id = UUID.randomUUID();
            var now = Instant.now();
            var promo = new Promotion(id, "Promo", now.minusSeconds(60), now.plusSeconds(3600), 100L, null, PromotionStatus.SCHEDULED, now);
            when(promotionPersistence.findScheduledOrActive()).thenReturn(List.of(promo));

            var result = promotionService.activateDuePromotions();

            assertEquals(1, result.size());
            verify(promotionPersistence).transitionStatus(id, PromotionStatus.ACTIVE);
        }

        @Test
        void shouldNotActivatePromo_whenStartsAtIsInTheFuture() {
            var id = UUID.randomUUID();
            var now = Instant.now();
            var promo = new Promotion(id, "Promo", now.plusSeconds(3600), now.plusSeconds(7200), 100L, null, PromotionStatus.SCHEDULED, now);
            when(promotionPersistence.findScheduledOrActive()).thenReturn(List.of(promo));

            var result = promotionService.activateDuePromotions();

            assertTrue(result.isEmpty());
            verify(promotionPersistence, never()).transitionStatus(any(), any());
        }

        @Test
        void shouldActivateMultipleEligiblePromos() {
            var now = Instant.now();
            var promo1 = new Promotion(UUID.randomUUID(), "P1", now.minusSeconds(120), now.plusSeconds(3600), 100L, null, PromotionStatus.SCHEDULED, now);
            var promo2 = new Promotion(UUID.randomUUID(), "P2", now.minusSeconds(60), now.plusSeconds(7200), 100L, null, PromotionStatus.SCHEDULED, now);
            when(promotionPersistence.findScheduledOrActive()).thenReturn(List.of(promo1, promo2));

            var result = promotionService.activateDuePromotions();

            assertEquals(2, result.size());
            verify(promotionPersistence, times(2)).transitionStatus(any(), eq(PromotionStatus.ACTIVE));
        }
    }

    // ─── ExpireDuePromotions ───────────────────────────────────────────────

    @Nested
    class ExpireDuePromotions {

        @Test
        void shouldExpirePromo_whenEndsAtIsInThePast() {
            var id = UUID.randomUUID();
            var now = Instant.now();
            var promo = new Promotion(id, "Promo", now.minusSeconds(7200), now.minusSeconds(3600), 100L, null, PromotionStatus.ACTIVE, now);
            when(promotionPersistence.findScheduledOrActive()).thenReturn(List.of(promo));

            var result = promotionService.expireDuePromotions();

            assertEquals(1, result.size());
            verify(promotionPersistence).transitionStatus(id, PromotionStatus.EXPIRED);
        }

        @Test
        void shouldNotExpirePromo_whenEndsAtIsInTheFuture() {
            var id = UUID.randomUUID();
            var now = Instant.now();
            var promo = new Promotion(id, "Promo", now.minusSeconds(60), now.plusSeconds(3600), 100L, null, PromotionStatus.ACTIVE, now);
            when(promotionPersistence.findScheduledOrActive()).thenReturn(List.of(promo));

            var result = promotionService.expireDuePromotions();

            assertTrue(result.isEmpty());
            verify(promotionPersistence, never()).transitionStatus(any(), any());
        }

        @Test
        void shouldNotExpireScheduledPromos() {
            var now = Instant.now();
            var sched = new Promotion(UUID.randomUUID(), "Sched", now.minusSeconds(7200), now.minusSeconds(3600), 100L, null, PromotionStatus.SCHEDULED, now);
            when(promotionPersistence.findScheduledOrActive()).thenReturn(List.of(sched));

            var result = promotionService.expireDuePromotions();

            assertTrue(result.isEmpty());
            verify(promotionPersistence, never()).transitionStatus(any(), any());
        }
    }

    // ─── GetConfig / UpdateConfig ──────────────────────────────────────────

    @Nested
    class GetConfig {

        @Test
        void shouldDelegateToPersistence() {
            when(superLikeConfigPersistence.get()).thenReturn(defaultConfig);

            var result = promotionService.getConfig();

            assertEquals(defaultConfig.id(), result.id());
            verify(superLikeConfigPersistence).get();
        }
    }

    @Nested
    class UpdateConfig {

        @Test
        void shouldDelegateToPersistenceAndReturn() {
            var updated = new SuperLikeConfig(UUID.randomUUID(), 200L, new BigDecimal("0.50"), Instant.now());
            when(superLikeConfigPersistence.update(200L, new BigDecimal("0.50"))).thenReturn(updated);

            var result = promotionService.updateConfig(200L, new BigDecimal("0.50"));

            assertEquals(200L, result.priceCents());
            assertEquals(new BigDecimal("0.50"), result.earningsRatio());
            verify(superLikeConfigPersistence).update(200L, new BigDecimal("0.50"));
        }
    }
}
