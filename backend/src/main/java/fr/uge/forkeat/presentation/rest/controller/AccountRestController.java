package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.user.*;
import fr.uge.forkeat.presentation.mapper.rest.UserDTOMapper;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.presentation.response.SuccessResponse;
import fr.uge.forkeat.service.PasswordValidator;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.port.PasswordHasher;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserService;
import fr.uge.forkeat.service.user.UserUpdateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountRestController {

    private final AuthenticationPort authPort;
    private final UserService userService;
    private final UserUpdateService userUpdateService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordHasher passwordHasher;

    public AccountRestController(AuthenticationPort authPort,
                                 UserService userService,
                                 UserUpdateService userUpdateService,
                                 EmailVerificationService emailVerificationService,
                                 PasswordHasher passwordHasher) {
        this.authPort = authPort;
        this.userService = userService;
        this.userUpdateService = userUpdateService;
        this.emailVerificationService = emailVerificationService;
        this.passwordHasher = passwordHasher;
    }

    @GetMapping
    public ResponseEntity<HttpResponse<UserDTO>> account() throws ResourceNotFoundException {
        var username = authPort.extractUsername();
        var user = userService.getUserByUsername(username);
        return ResponseEntity.ok(new ItemResponse<>(UserDTOMapper.toDTO(user)));
    }

    @PutMapping
    public ResponseEntity<HttpResponse<UserDTO>> updateProfile(
            @RequestBody UserUpdateProfileDTO userUpdateProfile) throws ResourceNotFoundException {
        if (userUpdateProfile.firstName().isBlank() || userUpdateProfile.lastName().isBlank() || userUpdateProfile.username().isBlank()) {
            throw new RegisterFailureException("Veuillez remplir tous les champs");
        }
        var currentUsername = authPort.extractUsername();
        var updatedUser = userUpdateService.updateProfile(
                currentUsername,
                userUpdateProfile.username(),
                userUpdateProfile.firstName(),
                userUpdateProfile.lastName());
        return ResponseEntity.ok(new ItemResponse<>(UserDTOMapper.toDTO(updatedUser)));
    }

    @PostMapping("/request-password-change")
    public ResponseEntity<HttpResponse<Void>> requestPasswordChange(
            @RequestBody PasswordChangeDTO passwordChangeDTO) {
        if (!passwordChangeDTO.newPassword().equals(passwordChangeDTO.confirmPassword())) {
            throw new RegisterFailureException("Les mots de passe ne correspondent pas");
        }
        var username = authPort.extractUsername();
        userUpdateService.requestPasswordChange(username, passwordChangeDTO.currentPassword(), passwordChangeDTO.newPassword());
        return ResponseEntity.ok(new SuccessResponse());
    }

    @PostMapping("/confirm-password-change")
    public ResponseEntity<HttpResponse<Void>> confirmPasswordChange(
            @RequestBody ConfirmPasswordChangeDTO dto) {
        if (!dto.newPassword().equals(dto.confirmPassword())) {
            throw new RegisterFailureException("Les mots de passe ne correspondent pas");
        }
        var username = authPort.extractUsername();
        var user = userService.getUserByUsername(username);
        var hashedPassword = passwordHasher.hash(dto.newPassword());
        emailVerificationService.confirmPasswordChange(user.id(), dto.code(), hashedPassword);
        return ResponseEntity.ok(new SuccessResponse());
    }

    @PostMapping("/request-email-change")
    public ResponseEntity<HttpResponse<Void>> requestEmailChange(
            @RequestBody RequestEmailChangeDTO dto) {
        var username = authPort.extractUsername();
        var user = userService.getUserByUsername(username);
        if (user.authMode() == AuthMode.GOOGLE) {
            if (dto.newPassword().length() < 8) {
                throw new RegisterFailureException("Vous devez définir un mot de passe (min 8 caractères) pour changer votre email.");
            }
            if (!dto.newPassword().equals(dto.confirmPassword())) {
                throw new RegisterFailureException("Les mots de passe ne correspondent pas");
            }
        }
        userUpdateService.requestEmailChange(username, dto.newEmail(), dto.currentPassword(), dto.newPassword());
        return ResponseEntity.ok(new SuccessResponse());
    }

    @PostMapping("/confirm-email-change")
    public ResponseEntity<HttpResponse<UserDTO>> confirmEmailChange(
            @RequestBody ConfirmCodeDTO dto) {
        var username = authPort.extractUsername();
        var user = userService.getUserByUsername(username);
        var updatedUser = emailVerificationService.confirmEmailChange(user.id(), dto.code());
        return ResponseEntity.ok(new ItemResponse<>(UserDTOMapper.toDTO(updatedUser)));
    }

    @PostMapping("/resend-confirmation")
    public ResponseEntity<HttpResponse<Void>> resendConfirmation() {
        var username = authPort.extractUsername();
        var user = userService.getUserByUsername(username);
        if (user.emailVerified()) {
            return ResponseEntity.ok(new SuccessResponse());
        }
        emailVerificationService.sendEmailConfirmation(user.id(), user.email());
        return ResponseEntity.ok(new SuccessResponse());
    }

    @PostMapping("/set-password")
    public ResponseEntity<HttpResponse<Void>> setPasswordForOAuthUser(
            @RequestBody SetPasswordDTO dto) {
        if (!dto.newPassword().equals(dto.confirmPassword())) {
            throw new RegisterFailureException("Les mots de passe ne correspondent pas");
        }
        PasswordValidator.validate(dto.newPassword());
        var username = authPort.extractUsername();
        userUpdateService.setPasswordForOAuthUser(username, dto.newPassword());
        return ResponseEntity.ok(new SuccessResponse());
    }
}