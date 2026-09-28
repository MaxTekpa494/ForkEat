package fr.uge.forkeat.service.model.webhook;

public sealed interface WebhookEvent
        permits CheckoutSessionCompletedEvent, TransferCreatedEvent,
                PayoutFailedEvent, UnknownWebhookEvent {
}