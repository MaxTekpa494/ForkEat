package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.DuplicateTransactionException;
import fr.uge.forkeat.service.exception.WithdrawalException;
import fr.uge.forkeat.service.exception.WalletNotFoundException;
import fr.uge.forkeat.service.external.PaymentGateway;
import fr.uge.forkeat.service.model.payment.PaymentRequest;
import fr.uge.forkeat.service.model.transaction.Transaction;
import fr.uge.forkeat.service.model.transaction.TransactionStatus;
import fr.uge.forkeat.service.model.transaction.TransactionType;
import fr.uge.forkeat.service.model.wallet.Currency;
import fr.uge.forkeat.service.model.wallet.Wallet;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import fr.uge.forkeat.service.external.PayoutGateway;
import fr.uge.forkeat.service.user.BankInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class WalletService {

	private final PaymentGateway paymentGateway;
	private final PayoutGateway payoutGateway;
	private final WalletPersistence walletPersistence;
	private final BankInfoService bankInfoService;
	private final TransactionTemplate txTemplate;

	public WalletService(PaymentGateway paymentGateway, PayoutGateway payoutGateway,
						 WalletPersistence walletPersistence, BankInfoService bankInfoService,
						 PlatformTransactionManager transactionManager) {
		this.paymentGateway = Objects.requireNonNull(paymentGateway);
		this.payoutGateway = Objects.requireNonNull(payoutGateway);
		this.walletPersistence = Objects.requireNonNull(walletPersistence);
		this.bankInfoService = Objects.requireNonNull(bankInfoService);

		this.txTemplate = new TransactionTemplate(Objects.requireNonNull(transactionManager));
		this.txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		this.txTemplate.setTimeout(30);
	}

	public String prepareTopUp(UUID userId, String email, Long amount, String source) {
		var request = new PaymentRequest(userId, email, amount, Currency.DEFAULT.code(), source);
		return paymentGateway.initiatePayment(request).paymentUrl();
	}

	@Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 30)
	public void processPaymentConfirmation(UUID userId, Long amount, String stripeTransactionID) {

		if (walletPersistence.transactionExists(stripeTransactionID)) {
			throw new DuplicateTransactionException(stripeTransactionID);
		}

		var wallet = walletPersistence.loadWalletWithLock(userId)
				.orElseThrow(() -> new WalletNotFoundException(userId));

		var newWallet = wallet.credit(amount);
		walletPersistence.saveWallet(newWallet);

		var trace = new Transaction(UUID.randomUUID(), (UUID)null, wallet.id(), amount, TransactionType.RECHARGE, Instant.now(),
				stripeTransactionID, TransactionStatus.SUCCEEDED);

		walletPersistence.saveTransaction(trace);
	}

	/**
	 * Requests a withdrawal:
	 * 1. Debit wallet + create PENDING transaction in a single TX.
	 * 2. Call Stripe to initiate the payout.
	 * The final status (SUCCEEDED / FAILED) is handled by the Stripe webhook
	 * via {@link #processPayoutConfirmation(String, boolean)}.
	 */
	public String requestWithdrawal(UUID userId, Long amount) {
		Objects.requireNonNull(userId);
		Objects.requireNonNull(amount);

		if (amount <= 0) {
			throw new WithdrawalException("Withdrawal amount must be positive.");
		}

		record WithdrawalContext(UUID pendingTxId, String connectAccountId) {}

		var ctx = txTemplate.execute(status -> {
			var bankInfo = bankInfoService.getBankInfoByUserId(userId)
					.orElseThrow(() -> new WithdrawalException("No bank information found for user " + userId + ". Please add bank details to withdraw funds."));

			var wallet = walletPersistence.loadWalletWithLock(userId)
					.orElseThrow(() -> new WalletNotFoundException(userId));

			if (wallet.balance() < amount) {
				throw new WithdrawalException("Insufficient balance for withdrawal. Current balance: " + wallet.balance() + ", requested: " + amount);
			}

			walletPersistence.saveWallet(wallet.debit(amount));

			var pendingTxId = UUID.randomUUID();
			walletPersistence.saveTransaction(new Transaction(pendingTxId, wallet.id(), (UUID) null, amount,
					TransactionType.WITHDRAWAL, Instant.now(), null, TransactionStatus.PENDING));

			return new WithdrawalContext(pendingTxId, bankInfo.externalAccountId());
		});

		// pendingTxId is passed in Stripe metadata so the webhook can find the transaction without race conditions
		return payoutGateway.initiatePayout(userId, ctx.pendingTxId(), amount, ctx.connectAccountId(), Currency.EUR);
	}

	/**
	 * Links a PENDING withdrawal transaction to its real Stripe Transfer ID and marks it as SUCCEEDED.
	 * Called from the transfer.created webhook handler.
	 * Looking up by pendingTxId (UUID stored in Stripe metadata) avoids any race condition.
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 30)
	public void linkAndConfirmPayout(String stripeTransferId, UUID pendingTxId) {
		Objects.requireNonNull(stripeTransferId);
		Objects.requireNonNull(pendingTxId);

		var transaction = walletPersistence.findTransactionById(pendingTxId)
				.orElseThrow(() -> new WithdrawalException("Pending transaction not found: " + pendingTxId));

		if (transaction.status() == TransactionStatus.SUCCEEDED) {
			return; // idempotency
		}

		walletPersistence.updateTransaction(new Transaction(
				transaction.id(), transaction.walletSourceId(), transaction.walletDestinationId(),
				transaction.amount(), transaction.type(), transaction.createdAt(),
				stripeTransferId, TransactionStatus.SUCCEEDED));
	}

	@Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 30)
	public void processPayoutConfirmation(String stripePayoutId, boolean succeeded) {
		Objects.requireNonNull(stripePayoutId);

		var transaction = walletPersistence.findTransactionByStripeTransactionID(stripePayoutId)
				.orElseThrow(() -> new WithdrawalException("Transaction not found for Stripe Payout ID: " + stripePayoutId));

		if (succeeded) {
			if (transaction.status() == TransactionStatus.SUCCEEDED) {
				return;
			}
			Transaction updatedTransaction = new Transaction(transaction.id(), transaction.walletSourceId(), transaction.walletDestinationId(),
					transaction.amount(), transaction.type(), transaction.createdAt(), transaction.stripeTransactionID(), TransactionStatus.SUCCEEDED);
			walletPersistence.updateTransaction(updatedTransaction);
		} else {
			if (transaction.status() == TransactionStatus.FAILED) {
				return;
			}
			Transaction updatedTransaction = new Transaction(transaction.id(), transaction.walletSourceId(), transaction.walletDestinationId(),
					transaction.amount(), transaction.type(), transaction.createdAt(), transaction.stripeTransactionID(), TransactionStatus.FAILED);
			walletPersistence.updateTransaction(updatedTransaction);

			if (transaction.walletSourceId() != null) {
				var wallet = walletPersistence.getWalletById(transaction.walletSourceId())
						.orElseThrow(() -> new WalletNotFoundException(transaction.walletSourceId()));
				var revertedWallet = wallet.credit(transaction.amount());
				walletPersistence.saveWallet(revertedWallet);
			}
		}
	}


	@Transactional(readOnly = true)
	public long getBalance(UUID userId) {
		return walletPersistence.getBalance(userId);
	}

	@Transactional(readOnly = true)
	public List<Transaction> getTransactionHistory(UUID userId) {
		return walletPersistence.getTransactionsByUserId(userId);
	}

	@Transactional(timeout = 10)
	public Wallet createWallet(UUID userId)  {
		var newWallet = new Wallet(UUID.randomUUID(), userId, 0L, Instant.now());
		return walletPersistence.saveWallet(newWallet);
	}
}