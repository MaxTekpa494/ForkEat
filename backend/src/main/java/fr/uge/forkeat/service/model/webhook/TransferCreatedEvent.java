package fr.uge.forkeat.service.model.webhook;

import java.util.UUID;

public record TransferCreatedEvent(
        String transferId,
        UUID pendingTxId    // null si absent des metadata (backward compat)
) implements WebhookEvent {
}