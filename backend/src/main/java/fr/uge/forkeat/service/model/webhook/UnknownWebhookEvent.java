package fr.uge.forkeat.service.model.webhook;

public record UnknownWebhookEvent(String type) implements WebhookEvent {
}