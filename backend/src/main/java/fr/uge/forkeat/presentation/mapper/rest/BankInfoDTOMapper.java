package fr.uge.forkeat.presentation.mapper.rest;

import fr.uge.forkeat.presentation.dto.user.BankInfoResponseDTO; // Changed import
import fr.uge.forkeat.service.model.user.BankInfo;

import java.util.Objects;

public class BankInfoDTOMapper {

  private BankInfoDTOMapper(){}

  public static BankInfoResponseDTO toResponseDTO(BankInfo bankInfo) {
    Objects.requireNonNull(bankInfo);
    return new BankInfoResponseDTO(
            bankInfo.userId(),
            bankInfo.bankName(),
            bankInfo.externalAccountId()
    );
  }
}