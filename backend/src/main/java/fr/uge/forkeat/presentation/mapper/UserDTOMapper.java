package fr.uge.forkeat.presentation.mapper;

import fr.uge.forkeat.presentation.dto.user.BankInfoDTO;
import fr.uge.forkeat.presentation.dto.user.UserDTO;
import fr.uge.forkeat.presentation.dto.user.WalletDTO;
import fr.uge.forkeat.service.model.user.BankInfo;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.Wallet;
import org.springframework.stereotype.Component;

@Component
public final class UserDTOMapper {

    public UserDTOMapper() {}

    // C'est mieux on Optional non ?
    public  UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(
                user.id(),
                user.username(),
                user.firstName(),
                user.lastName(),
                user.email(),
                user.role().name(),
                user.status().name(),
                user.authMode().name(),
                toBankInfoDTO(user.bankInfo()),
                toWalletDTO(user.wallet()),
                user.createdAt(),
                user.updatedAt()
        );
    }

    public  WalletDTO toWalletDTO(Wallet wallet) {
        if (wallet == null) {
            return null;
        }
        return new WalletDTO(
                wallet.id(),
                wallet.balance(),
                wallet.updatedAt()
        );
    }

    public BankInfoDTO toBankInfoDTO(BankInfo bankInfo) {
        if (bankInfo == null) {
            return null;
        }
        return new BankInfoDTO(
                bankInfo.bankName(),
                maskIban(bankInfo.iban()),
                bankInfo.bic()
        );
    }

    private String maskIban(String iban) {
        if (iban == null || iban.length() <= 8) {
            return "****";
        }
        return iban.substring(0, 4) + "****" + iban.substring(iban.length() - 4);
    }
}
