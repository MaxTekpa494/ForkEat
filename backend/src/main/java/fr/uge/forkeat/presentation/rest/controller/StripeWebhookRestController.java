package fr.uge.forkeat.presentation.rest.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.checkout.Session;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.external.PaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

@RestController
@RequestMapping("/wallet/webhooks/stripe")
public class StripeWebhookRestController {

	private final WalletService walletService;
	private final PaymentGateway paymentGateway;

	@Value("${stripe.webhook.secret}")
	private String endpointSecret;

	private final Logger logger = Logger.getLogger(StripeWebhookRestController.class.getName());
	private final String stripeSessionResponse = "checkout.session.completed";

	public StripeWebhookRestController(WalletService walletService, PaymentGateway paymentGateway) {
		this.walletService = Objects.requireNonNull(walletService);
		this.paymentGateway = Objects.requireNonNull(paymentGateway);
	}

	@PostMapping
	public ResponseEntity<String> handleStripeEvent(@RequestBody String payload,
			@RequestHeader("Stripe-Signature") String sigHeader) {
		Objects.requireNonNull(payload);
		Objects.requireNonNull(sigHeader);

		var event = paymentGateway.initEvent(payload, sigHeader, endpointSecret);

		if (stripeSessionResponse.equals(event.getType())) {

			var session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
			assert session != null; // Je vais investiguer
			var stripeTxId = session.getId();
			var userId = UUID.fromString(session.getMetadata().get("userId"));
			var amount = session.getAmountTotal(); // En centimes

			// try {
			walletService.processPaymentConfirmation(userId, amount, stripeTxId);
			// } catch (Exception e) {
			// return ResponseEntity.internalServerError().build();
			// }
		}

		// APRÈS ON CHANGE
		return ResponseEntity.ok().build(); // On envoie a stripe un 200 OK
	}
}