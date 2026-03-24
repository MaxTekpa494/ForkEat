package fr.uge.forkeat.service.model.webhook;

public record PayoutFailedEvent(
        String payoutId,
        String transferId,      // null si absent des metadata
        String failureMessage
) implements WebhookEvent {
}