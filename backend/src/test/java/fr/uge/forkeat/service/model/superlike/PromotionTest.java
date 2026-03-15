package fr.uge.forkeat.service.model.superlike;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PromotionTest {

    private Promotion valid() {
        var now = Instant.now();
        return new Promotion(UUID.randomUUID(), "Promo", now, now.plusSeconds(3600), 100L, null, PromotionStatus.SCHEDULED, now);
    }

    @Nested
    class ConstructorValidation {

        @Test
        void shouldCreateValidPromotion() {
            assertDoesNotThrow(PromotionTest.this::valid);
        }

        @Test
        void shouldThrowNPE_whenIdIsNull() {
            var now = Instant.now();
            assertThrows(NullPointerException.class, () ->
                    new Promotion(null, "Promo", now, now.plusSeconds(3600), 100L, null, PromotionStatus.SCHEDULED, now));
        }

        @Test
        void shouldThrowNPE_whenNameIsNull() {
            var now = Instant.now();
            assertThrows(NullPointerException.class, () ->
                    new Promotion(UUID.randomUUID(), null, now, now.plusSeconds(3600), 100L, null, PromotionStatus.SCHEDULED, now));
        }

        @Test
        void shouldThrowNPE_whenStartsAtIsNull() {
            var now = Instant.now();
            assertThrows(NullPointerException.class, () ->
                    new Promotion(UUID.randomUUID(), "Promo", null, now.plusSeconds(3600), 100L, null, PromotionStatus.SCHEDULED, now));
        }

        @Test
        void shouldThrowNPE_whenEndsAtIsNull() {
            var now = Instant.now();
            assertThrows(NullPointerException.class, () ->
                    new Promotion(UUID.randomUUID(), "Promo", now, null, 100L, null, PromotionStatus.SCHEDULED, now));
        }

        @Test
        void shouldThrowNPE_whenStatusIsNull() {
            var now = Instant.now();
            assertThrows(NullPointerException.class, () ->
                    new Promotion(UUID.randomUUID(), "Promo", now, now.plusSeconds(3600), 100L, null, null, now));
        }

        @Test
        void shouldThrowNPE_whenCreatedAtIsNull() {
            var now = Instant.now();
            assertThrows(NullPointerException.class, () ->
                    new Promotion(UUID.randomUUID(), "Promo", now, now.plusSeconds(3600), 100L, null, PromotionStatus.SCHEDULED, null));
        }

        @Test
        void shouldThrowIllegalArgument_whenPriceCentsIsZero() {
            var now = Instant.now();
            assertThrows(IllegalArgumentException.class, () ->
                    new Promotion(UUID.randomUUID(), "Promo", now, now.plusSeconds(3600), 0L, null, PromotionStatus.SCHEDULED, now));
        }

        @Test
        void shouldThrowIllegalArgument_whenPriceCentsIsNegative() {
            var now = Instant.now();
            assertThrows(IllegalArgumentException.class, () ->
                    new Promotion(UUID.randomUUID(), "Promo", now, now.plusSeconds(3600), -1L, null, PromotionStatus.SCHEDULED, now));
        }

        @Test
        void shouldThrowIllegalArgument_whenEndsAtEqualsStartsAt() {
            var now = Instant.now();
            assertThrows(IllegalArgumentException.class, () ->
                    new Promotion(UUID.randomUUID(), "Promo", now, now, 100L, null, PromotionStatus.SCHEDULED, now));
        }

        @Test
        void shouldThrowIllegalArgument_whenEndsAtBeforeStartsAt() {
            var now = Instant.now();
            assertThrows(IllegalArgumentException.class, () ->
                    new Promotion(UUID.randomUUID(), "Promo", now, now.minusSeconds(1), 100L, null, PromotionStatus.SCHEDULED, now));
        }

        @Test
        void shouldThrowIllegalArgument_whenBonusEveryNIsOne() {
            var now = Instant.now();
            assertThrows(IllegalArgumentException.class, () ->
                    new Promotion(UUID.randomUUID(), "Promo", now, now.plusSeconds(3600), 100L, 1, PromotionStatus.SCHEDULED, now));
        }

        @Test
        void shouldAcceptBonusEveryNOfTwo() {
            var now = Instant.now();
            assertDoesNotThrow(() ->
                    new Promotion(UUID.randomUUID(), "Promo", now, now.plusSeconds(3600), 100L, 2, PromotionStatus.SCHEDULED, now));
        }

        @Test
        void shouldAcceptNullBonusEveryN() {
            var now = Instant.now();
            assertDoesNotThrow(() ->
                    new Promotion(UUID.randomUUID(), "Promo", now, now.plusSeconds(3600), 100L, null, PromotionStatus.SCHEDULED, now));
        }
    }

    @Nested
    class IsModifiable {

        @Test
        void shouldBeTrue_whenScheduled() {
            assertTrue(valid().isModifiable());
        }

        @Test
        void shouldBeFalse_whenActive() {
            var now = Instant.now();
            var promo = new Promotion(UUID.randomUUID(), "Promo", now, now.plusSeconds(3600), 100L, null, PromotionStatus.ACTIVE, now);
            assertFalse(promo.isModifiable());
        }

        @Test
        void shouldBeFalse_whenExpired() {
            var now = Instant.now();
            var promo = new Promotion(UUID.randomUUID(), "Promo", now, now.plusSeconds(3600), 100L, null, PromotionStatus.EXPIRED, now);
            assertFalse(promo.isModifiable());
        }

        @Test
        void shouldBeFalse_whenCancelled() {
            var now = Instant.now();
            var promo = new Promotion(UUID.randomUUID(), "Promo", now, now.plusSeconds(3600), 100L, null, PromotionStatus.CANCELLED, now);
            assertFalse(promo.isModifiable());
        }
    }
}
