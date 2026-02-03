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
    return new UserDTO(
            user.username(),
            user.firstName(),
            user.lastName(),
            user.email(),
            user.role().name(), user.status().name(), user.authMode().name(),
            user.createdAt(), user.updatedAt());
  }

  public static UserRegister toUserRegister(UserRegisterDTO dto) {
    Objects.requireNonNull(dto);
    return new UserRegister(dto.username(), dto.firstName(), dto.lastName(), dto.password(), dto.email());
  }


  public static UserLogin toUserLogin(UserLoginDTO dto) {
    Objects.requireNonNull(dto);
    return new UserLogin(dto.username(), dto.password());
  }

}
