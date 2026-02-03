package fr.uge.forkeat.presentation.mapper.rest;

import fr.uge.forkeat.presentation.dto.user.BankInfoDTO;
import fr.uge.forkeat.presentation.dto.user.UserDTO;
import fr.uge.forkeat.presentation.dto.user.UserRegisterDTO;
import fr.uge.forkeat.presentation.dto.user.WalletDTO;
import fr.uge.forkeat.service.model.user.BankInfo;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRegister;
import fr.uge.forkeat.service.model.user.Wallet;
import fr.uge.forkeat.presentation.dto.user.UserLoginDTO;
import fr.uge.forkeat.service.model.user.UserLogin;

import java.util.Objects;

import org.springframework.stereotype.Component;

public final class UserDTOMapper {

	private UserDTOMapper() {
	}

	// C'est mieux on Optional non ?
	public static UserDTO toDTO(User user) {
		Objects.requireNonNull(user);
		return new UserDTO(user.id(), user.username(), user.firstName(), user.lastName(), user.email(),
				user.role().name(), user.status().name(), user.authMode().name(), user.walletId(), user.bankInfoId(),
				user.createdAt(), user.updatedAt());
	}

	public static UserRegister toUserRegister(UserRegisterDTO dto) {
		Objects.requireNonNull(dto);
		return new UserRegister(dto.username(), dto.firstName(), dto.lastName(), dto.password(), dto.email());
	}

	public static UserRegisterDTO toUserRegisterDTO(UserRegister userRegister) {
		Objects.requireNonNull(userRegister);
		return new UserRegisterDTO(userRegister.username(), userRegister.firstName(), userRegister.lastName(),
				userRegister.password(), userRegister.email());
	}

	public static UserLoginDTO toUserLoginDTO(UserLogin userLogin) {
		Objects.requireNonNull(userLogin);

		return new UserLoginDTO(userLogin.username(), userLogin.password()); // Max ici on met mot de passe ?
	}

	public static UserLogin toUserLogin(UserLoginDTO dto) {
		Objects.requireNonNull(dto);
		return new UserLogin(dto.username(), dto.password());
	}

	// private static WalletDTO toWalletDTO(Wallet wallet) {
	// if (wallet == null) {
	// return null;
	// }
	// return new WalletDTO(
	// wallet.id(),
	// wallet.balance(),
	// wallet.updatedAt()
	// );
	// }

	// public BankInfoDTO toBankInfoDTO(BankInfo bankInfo) {
	// if (bankInfo == null) {
	// return null;
	// }
	// return new BankInfoDTO(
	// bankInfo.bankName(),
	// maskIban(bankInfo.iban()),
	// bankInfo.bic()
	// );
	// }

	// private static String maskIban(String iban) {
	// if (iban == null || iban.length() <= 8) {
	// return "****";
	// }
	// return iban.substring(0, 4) + "****" + iban.substring(iban.length() - 4);
	// }

}
