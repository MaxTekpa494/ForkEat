package fr.uge.forkeat.service.exception;

import java.util.UUID;

public class WalletNotFoundException extends ResourceNotFoundException {
	public WalletNotFoundException(UUID walletId) {
		super("Wallet not found: " + walletId);
	}
}
