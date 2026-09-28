package fr.uge.forkeat.infrastructure.payment.mapper;

import com.stripe.model.Event;
import com.stripe.model.Payout;
import com.stripe.model.Transfer;
import com.stripe.model.checkout.Session;
import fr.uge.forkeat.service.model.webhook.*;

import java.util.UUID;

public class StripeEventMapper {

    private StripeEventMapper() {}

    public static WebhookEvent toWebhookEvent(Event event) {
        return switch (event.getType()) {
            case "checkout.session.completed" -> mapCheckoutSession(event);
            case "transfer.created"           -> mapTransfer(event);
            case "payout.failed"              -> mapPayout(event);
            default                           -> new UnknownWebhookEvent(event.getType());
        };
    }

    private static WebhookEvent mapCheckoutSession(Event event) {
        var session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) {
            return new UnknownWebhookEvent("checkout.session.completed#invalid");
        }
        return new CheckoutSessionCompletedEvent(
                session.getId(),
                UUID.fromString(session.getMetadata().get("userId")),
                session.getAmountTotal(),
                session.getPaymentStatus()
        );
    }

    private static WebhookEvent mapTransfer(Event event) {
        var deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isEmpty()) {
            return new UnknownWebhookEvent("transfer.created#api-mismatch");
        }
        var transfer = (Transfer) deserializer.getObject().get();
        var metadata = transfer.getMetadata();
        var pendingTxIdStr = metadata != null ? metadata.get("pendingTxId") : null;
        var pendingTxId = pendingTxIdStr != null ? UUID.fromString(pendingTxIdStr) : null;
        return new TransferCreatedEvent(transfer.getId(), pendingTxId);
    }

    private static WebhookEvent mapPayout(Event event) {
        var payout = (Payout) event.getDataObjectDeserializer().getObject().orElse(null);
        if (payout == null) {
            return new UnknownWebhookEvent("payout.failed#invalid");
        }
        var transferId = payout.getMetadata() != null ? payout.getMetadata().get("transferId") : null;
        return new PayoutFailedEvent(payout.getId(), transferId, payout.getFailureMessage());
    }
}