package fr.uge.forkeat.presentation.rest.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import fr.uge.forkeat.service.WalletService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
@RequestMapping("/wallet/webhooks/stripe")
public class StripeWebhookController {

    private static final Logger LOGGER = Logger.getLogger(StripeWebhookController.class.getName());

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final WalletService walletService;

    public StripeWebhookController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<String> handleStripeEvent(@RequestBody String payload, 
                                                    @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            // On vérifier que l'appel vient bien de Stripe Si la signature ne correspond pas à notre secret, on lance une exception
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature invalide");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur parsing");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            var session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);

            if (session != null) {
                handleCheckoutSession(session);
            }
        }

        return ResponseEntity.ok("Received");
    }


    private void handleCheckoutSession(Session session) {
        // Récupérer l'ID utilisateur qu'on avait caché dans les métadonnées lors du paiement
        var userIdStr = session.getMetadata().get("userId");

        var externalTransactionId = session.getId();

        // Récupérer le montant
        var amountInCents = session.getAmountTotal();
        
        if (userIdStr != null && amountInCents != null) {
            var userId = Long.parseLong(userIdStr);
            
            try {
                walletService.processPaymentConfirmation(userId, amountInCents, externalTransactionId);
                LOGGER.info("Succès : Wallet crédité pour user " + userId + " de " + amountInCents + "€");
            
            } catch (Exception e) {
                LOGGER.severe("Erreur critique lors du crédit wallet : " + e.getMessage());
            }
        }
    }
}