package fr.uge.forkeat.presentation.external;

import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.DuplicateTransactionException;
import fr.uge.forkeat.service.exception.WalletNotFoundException;
import fr.uge.forkeat.service.exception.WithdrawalException;
import fr.uge.forkeat.service.external.PaymentGateway;
import fr.uge.forkeat.service.model.webhook.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/wallet/webhooks/stripe")
public class StripeWebhook {

	private static final Logger log = LoggerFactory.getLogger(StripeWebhook.class);

	private final WalletService walletService;
	private final PaymentGateway paymentGateway;

	@Value("${stripe.webhook.secret}")
	private String endpointSecret;

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

		var event = paymentGateway.parseWebhookEvent(payload, sigHeader, endpointSecret);

		switch (event) {
			case CheckoutSessionCompletedEvent e -> handleCheckoutSessionCompleted(e);
			case TransferCreatedEvent e          -> handleTransferCreated(e);
			case PayoutFailedEvent e             -> { if (connectedAccountId != null) handlePayoutFailed(e); }
			case UnknownWebhookEvent e           -> {
				log.debug("Event Stripe ignore: {}", e.type());
				return ResponseEntity.ok("Event ignored");
			}
		}

		return ResponseEntity.ok("OK");
	}

	private void handleCheckoutSessionCompleted(CheckoutSessionCompletedEvent event) {
		if (!"paid".equals(event.paymentStatus())) {
			log.info("Session completed mais paiement non collecte (status={}), en attente du webhook payment_intent.succeeded", event.paymentStatus());
			return;
		}

		log.info("Traitement paiement: userId={}, amount={}, stripeTxId={}", event.userId(), event.amountTotal(), event.sessionId());

		try {
			walletService.processPaymentConfirmation(event.userId(), event.amountTotal(), event.sessionId());
			log.info("Paiement traite avec succes: stripeTxId={}", event.sessionId());
		} catch (DuplicateTransactionException e) {
			log.info("Transaction deja traitee (idempotence): stripeTxId={}", event.sessionId());
		} catch (WalletNotFoundException e) {
			log.error("Wallet introuvable pour userId={}: {}", event.userId(), e.getMessage());
		} catch (Exception e) {
			log.error("Erreur inattendue lors du traitement du paiement: stripeTxId={}", event.sessionId(), e);
		}
	}

	private void handleTransferCreated(TransferCreatedEvent event) {
		log.info("Transfer Stripe confirme: transferId={}", event.transferId());

		try {
			if (event.pendingTxId() != null) {
				walletService.linkAndConfirmPayout(event.transferId(), event.pendingTxId());
			} else {
				log.warn("Transfer {} n'a pas de pendingTxId dans les metadata, fallback stripeTransactionID", event.transferId());
				walletService.processPayoutConfirmation(event.transferId(), true);
			}
		} catch (WithdrawalException e) {
			log.error("Transaction introuvable pour transferId={}: {}", event.transferId(), e.getMessage());
		} catch (Exception e) {
			log.error("Erreur inattendue lors de la confirmation du transfer: transferId={}", event.transferId(), e);
		}
	}

	private void handlePayoutFailed(PayoutFailedEvent event) {
		log.warn("Payout bancaire echoue: payoutId={}, transferId={}, raison={}", event.payoutId(), event.transferId(), event.failureMessage());

		if (event.transferId() == null) {
			log.error("Impossible de retrouver le transferId dans les metadata du payout: payoutId={}", event.payoutId());
			return;
		}

		try {
			walletService.processPayoutConfirmation(event.transferId(), false);
		} catch (WithdrawalException e) {
			log.error("Erreur lors de la gestion de l'echec du payout: transferId={}, {}", event.transferId(), e.getMessage());
		} catch (Exception e) {
			log.error("Erreur inattendue lors de la gestion de l'echec du payout: transferId={}", event.transferId(), e);
		}
	}
}