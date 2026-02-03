package fr.uge.forkeat.presentation.rest.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook; // -- ici
import fr.uge.forkeat.service.WalletService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

@RestController
@RequestMapping("/wallet/webhooks/stripe")
public class StripeWebhookController {

	private final WalletService walletService;
	@Value("${stripe.webhook.secret}")
	private String endpointSecret;

	private final Logger logger = Logger.getLogger(StripeWebhookController.class.getName());
	private final String stripeSessionResponse = "checkout.session.completed";

	public StripeWebhookController(WalletService walletService) {
		this.walletService = Objects.requireNonNull(walletService);
	}

	@PostMapping
	public ResponseEntity<String> handleStripeEvent(@RequestBody String payload,
			@RequestHeader("Stripe-Signature") String sigHeader) {

		// Event event;
		// try {
		// On peut catch mais dans un service et on transforme une une exception
		//
		// event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
		/*
		 * } catch (SignatureVerificationException e) { // ON LAISSE ET ON ON VERRA
		 * APRÈS return ResponseEntity.badRequest().build(); }
		 */
		Objects.requireNonNull(payload);
		Objects.requireNonNull(sigHeader);
		var event = walletService.initEvent(payload, sigHeader, endpointSecret);

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