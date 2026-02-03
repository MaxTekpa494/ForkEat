package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.DuplicateTransactionException;
import fr.uge.forkeat.service.exception.WalletNotFoundException;
import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.StripEventException;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import fr.uge.forkeat.service.external.PaymentGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

import java.time.Instant;
import java.util.UUID;

@Service
public class WalletService {

	private final PaymentGateway paymentGateway;
	private final WalletPersistence walletPersistence;

	public WalletService(PaymentGateway paymentGateway, WalletPersistence walletPersistence) {
		this.paymentGateway = paymentGateway;
		this.walletPersistence = walletPersistence;
	}

	public Event initEvent(String payload, String sigHeader, String endpointSecret) {
		try {
			return Webhook.constructEvent(payload, sigHeader, endpointSecret);
		} catch (SignatureVerificationException e) {
			throw new StripEventException("Invalid Stripe signature", e);
		}
	}

	public String prepareTopUp(UUID userId, String email, Long amount) {
		var request = new PaymentRequest(userId, email, amount, Currency.DEFAULT.code());
		return paymentGateway.initiatePayment(request).paymentUrl();
	}

	@Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 30) // Le timeout evit le deadlock
	public void processPaymentConfirmation(UUID userId, Long amount, String stripeTransactionID) {

		// IDEMPOTENCE
		if (walletPersistence.transactionExists(stripeTransactionID)) { // Le throw vient de là
			throw new DuplicateTransactionException(stripeTransactionID);
		}

		// VERROUILLAGE PESSIMISTE
		var wallet = walletPersistence.loadWalletWithLock(userId) // ou la
				.orElseThrow(() -> new WalletNotFoundException(userId));

		var newWallet = wallet.addFunds(amount);
		walletPersistence.saveWallet(newWallet);

		// TRACABILITÉ
		var trace = new Transaction(null, wallet.id(), amount, TransactionType.RECHARGE, Instant.now(),
				stripeTransactionID);

		walletPersistence.saveTransaction(trace);
	}

	@Transactional(readOnly = true)
	public Long getBalance(UUID userId) {
		return walletPersistence.getBalance(userId);
	}

	/**
	 * Crée un nouveau wallet pour un utilisateur.
	 */
	@Transactional(isolation = Isolation.READ_COMMITTED, timeout = 10)
	public Wallet createWallet(UUID userId) throws ResourceNotFoundException {
		var newWallet = new Wallet(UUID.randomUUID(), 0L, userId, Instant.now());
		return walletPersistence.saveWallet(newWallet);
	}
}