package fr.uge.forkeat.presentation.external;

import com.stripe.model.Event;
import com.stripe.model.Payout;
import com.stripe.model.Transfer;
import com.stripe.model.checkout.Session;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.DuplicateTransactionException;
import fr.uge.forkeat.service.exception.WalletNotFoundException;
import fr.uge.forkeat.service.exception.WithdrawalException;
import fr.uge.forkeat.service.external.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/wallet/webhooks/stripe")
public class StripeWebhook {

	private static final Logger log = LoggerFactory.getLogger(StripeWebhook.class);

	private final WalletService walletService;
	private final PaymentGateway paymentGateway;

	@Value("${stripe.webhook.secret}")
	private String endpointSecret;

	private static final String CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";
	private static final String TRANSFER_CREATED = "transfer.created";
	private static final String PAYOUT_FAILED = "payout.failed";

	public StripeWebhook(WalletService walletService, PaymentGateway paymentGateway) {
		this.walletService = Objects.requireNonNull(walletService);
		this.paymentGateway = Objects.requireNonNull(paymentGateway);
	}

	@PostMapping
	public ResponseEntity<String> handleStripeEvent(
			@RequestBody String payload,
			@RequestHeader("Stripe-Signature") String sigHeader,
			@RequestHeader(value = "Stripe-Account", required = false) String connectedAccountId) {

		Objects.requireNonNull(payload);
		Objects.requireNonNull(sigHeader);

		log.debug("Webhook Stripe recu, connectedAccount={}", connectedAccountId);

		Event event = paymentGateway.initEvent(payload, sigHeader, endpointSecret);

		switch (event.getType()) {
			case CHECKOUT_SESSION_COMPLETED -> handleCheckoutSessionCompleted(event);
			case TRANSFER_CREATED -> handleTransferCreated(event);
			case PAYOUT_FAILED ->{
				if (connectedAccountId != null) {
					handlePayoutFailed(event);
				}
			}
			default -> {
				log.debug("Event Stripe ignore: {}", event.getType());
				return ResponseEntity.ok("Event ignored");
			}
		}

		return ResponseEntity.ok("OK");
	}

	private void handleCheckoutSessionCompleted(Event event) {
		var session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
		if (session == null) {
			log.warn("Session nulle dans l'event checkout.session.completed");
			return;
		}

		// Only process sessions where payment was actually collected.
		// For async payment methods (SEPA, Sofort…), Stripe sends checkout.session.completed
		// with payment_status="unpaid"; payment_intent.succeeded will follow later.
		if (!"paid".equals(session.getPaymentStatus())) {
			log.info("Session completed mais paiement non collecte (status={}), en attente du webhook payment_intent.succeeded", session.getPaymentStatus());
			return;
		}

		var stripeTxId = session.getId();
		var userId = UUID.fromString(session.getMetadata().get("userId"));
		var amount = session.getAmountTotal();

		log.info("Traitement paiement: userId={}, amount={}, stripeTxId={}", userId, amount, stripeTxId);

		try {
			walletService.processPaymentConfirmation(userId, amount, stripeTxId);
			log.info("Paiement traite avec succes: stripeTxId={}", stripeTxId);
		} catch (DuplicateTransactionException e) {
			log.info("Transaction deja traitee (idempotence): stripeTxId={}", stripeTxId);
		} catch (WalletNotFoundException e) {
			log.error("Wallet introuvable pour userId={}: {}", userId, e.getMessage());
		} catch (Exception e) {
			log.error("Erreur inattendue lors du traitement du paiement: stripeTxId={}", stripeTxId, e);
		}
	}

	private void handleTransferCreated(Event event) {
		var deserializer = event.getDataObjectDeserializer();
		if (!deserializer.getObject().isPresent()) {
			// API version mismatch: fall back to raw JSON deserialization
			log.warn("Deserialisation du Transfer echouee (mismatch API version?), tentative via rawJson. EventId={}", event.getId());
			var rawJson = deserializer.getRawJson();
			log.debug("Raw JSON Transfer: {}", rawJson);
			// Cannot proceed without the Transfer object
			return;
		}

		var transfer = (Transfer) deserializer.getObject().get();
		var transferId = transfer.getId();
		log.info("Transfer Stripe confirme: transferId={}", transferId);

		// Retrieve the pendingTxId stored in metadata during requestWithdrawal
		var metadata = transfer.getMetadata();
		var pendingTxIdStr = metadata != null ? metadata.get("pendingTxId") : null;
		log.info("Transfer metadata: pendingTxId={}, metadata={}", pendingTxIdStr, metadata);

		try {
			if (pendingTxIdStr != null) {
				var pendingTxId = UUID.fromString(pendingTxIdStr);
				walletService.linkAndConfirmPayout(transferId, pendingTxId);
			} else {
				// Fallback for transfers without pendingTxId metadata (backward compat)
				log.warn("Transfer {} n'a pas de pendingTxId dans les metadata, fallback stripeTransactionID", transferId);
				walletService.processPayoutConfirmation(transferId, true);
			}
		} catch (WithdrawalException e) {
			log.error("Transaction introuvable pour transferId={}: {}", transferId, e.getMessage());
		} catch (Exception e) {
			log.error("Erreur inattendue lors de la confirmation du transfer: transferId={}", transferId, e);
		}
	}

	private void handlePayoutFailed(Event event) {
		var payout = (Payout) event.getDataObjectDeserializer().getObject().orElse(null);
		if (payout == null) {
			log.warn("Payout null dans l'event payout.failed");
			return;
		}

		// The metadata on the payout contains the transferId set during initiatePayout
		var transferId = payout.getMetadata() != null ? payout.getMetadata().get("transferId") : null;
		var failureMessage = payout.getFailureMessage();
		log.warn("Payout bancaire echoue: payoutId={}, transferId={}, raison={}", payout.getId(), transferId, failureMessage);

		if (transferId == null) {
			log.error("Impossible de retrouver le transferId dans les metadata du payout: payoutId={}", payout.getId());
			return;
		}

		try {
			walletService.processPayoutConfirmation(transferId, false);
		} catch (WithdrawalException e) {
			log.error("Erreur lors de la gestion de l'echec du payout: transferId={}, {}", transferId, e.getMessage());
		} catch (Exception e) {
			log.error("Erreur inattendue lors de la gestion de l'echec du payout: transferId={}", transferId, e);
		}
	}
}
