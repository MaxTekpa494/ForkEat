package fr.uge.forkeat.presentation.mapper.rest;

import fr.uge.forkeat.presentation.dto.user.BankInfoDTO;
import fr.uge.forkeat.service.model.user.BankInfo;

import java.util.Objects;

public class BankInfoDTOMapper {

  private BankInfoDTOMapper(){}


  public static BankInfo toDomain(BankInfoDTO bankInfoDTO) {
    Objects.requireNonNull(bankInfoDTO);
    return new BankInfo(bankInfoDTO.userId(), bankInfoDTO.bankName(), bankInfoDTO.maskedIban(), bankInfoDTO.bic());
  }

  public static BankInfoDTO toDTO(BankInfo bankInfo) {
    Objects.requireNonNull(bankInfo);
    return new BankInfoDTO(
            bankInfo.userId(),
            bankInfo.bankName(),
            maskIban(bankInfo.iban()),
            bankInfo.bic()
    );
  }

  private static String maskIban(String iban) {
    if (iban == null || iban.length() <= 8) {
      return "****";
    }
    return iban.substring(0, 4) + "****" + iban.substring(iban.length() - 4);
  }

}
