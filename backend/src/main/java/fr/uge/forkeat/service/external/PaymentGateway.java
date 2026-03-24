package fr.uge.forkeat.service.external;

import fr.uge.forkeat.service.model.payment.PaymentRequest;
import fr.uge.forkeat.service.model.payment.PaymentResponse;
import fr.uge.forkeat.service.model.webhook.WebhookEvent;

public interface PaymentGateway {

    /**
     * Demande au système de paiement externe (STRIPE) de préparer une session.
     * @param request Les détails
     * @return La réponse contenant l'URL de redirection
     */
    PaymentResponse initiatePayment(PaymentRequest request);

    /**
     * Vérifie la signature du webhook et retourne un événement domaine.
     * Toute connaissance de Stripe reste confinée dans l'implémentation infrastructure.
     */
    WebhookEvent parseWebhookEvent(String payload, String sigHeader, String endpointSecret);
}