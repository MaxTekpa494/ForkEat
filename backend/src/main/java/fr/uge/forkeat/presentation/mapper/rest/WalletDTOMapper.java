package fr.uge.forkeat.presentation.mapper.rest;

import fr.uge.forkeat.presentation.dto.user.WalletDTO;
import fr.uge.forkeat.service.model.wallet.Wallet;

import java.util.Objects;

public class WalletDTOMapper {

  private WalletDTOMapper(){}

  public static WalletDTO toDTO(Wallet wallet) {
    Objects.requireNonNull(wallet);
    return new WalletDTO(wallet.id(), wallet.userId(), wallet.balance(), wallet.updatedAt());
  }

  private static Wallet toDomain(WalletDTO dto) {
    Objects.requireNonNull(dto);
    return new Wallet(dto.id(), dto.userId(), dto.balance(), dto.updatedAt());
  }
}
