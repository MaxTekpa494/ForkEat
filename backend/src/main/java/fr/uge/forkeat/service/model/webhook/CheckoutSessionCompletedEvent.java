package fr.uge.forkeat.service.model.webhook;

import java.util.UUID;

public record CheckoutSessionCompletedEvent(
        String sessionId,
        UUID userId,
        long amountTotal,
        String paymentStatus
) implements WebhookEvent {
}