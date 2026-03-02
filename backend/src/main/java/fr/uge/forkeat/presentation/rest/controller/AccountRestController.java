package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.user.PasswordChangeDTO;
import fr.uge.forkeat.presentation.dto.user.RequestEmailChangeDTO;
import fr.uge.forkeat.presentation.dto.user.SetPasswordDTO;
import fr.uge.forkeat.presentation.dto.user.UserDTO;
import fr.uge.forkeat.presentation.dto.user.UserUpdateProfileDTO;
import fr.uge.forkeat.presentation.mapper.rest.UserDTOMapper;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.presentation.response.SuccessResponse;
import fr.uge.forkeat.service.port.AuthenticationPort;
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

    public AccountRestController(AuthenticationPort authPort,
                                 UserService userService,
                                 UserUpdateService userUpdateService,
                                 EmailVerificationService emailVerificationService) {
        this.authPort = authPort;
        this.userService = userService;
        this.userUpdateService = userUpdateService;
        this.emailVerificationService = emailVerificationService;
    }

    @GetMapping
    public ResponseEntity<HttpResponse<UserDTO>> account() {
        var username = authPort.extractUsername();
        var user = userService.getUserByUsername(username);
        return ResponseEntity.ok(new ItemResponse<>(UserDTOMapper.toDTO(user)));
    }

    @PutMapping
    public ResponseEntity<HttpResponse<UserDTO>> updateProfile(
            @RequestBody UserUpdateProfileDTO userUpdateProfile) {
        var currentUsername = authPort.extractUsername();
        var updatedUser = userUpdateService.updateProfile(
                currentUsername,
                userUpdateProfile.username(),
                userUpdateProfile.firstName(),
                userUpdateProfile.lastName());
        return ResponseEntity.ok(new ItemResponse<>(UserDTOMapper.toDTO(updatedUser)));
    }

    @PostMapping("/password-change-requests")
    public ResponseEntity<HttpResponse<Void>> requestPasswordChange(
            @RequestBody PasswordChangeDTO passwordChangeDTO) {
        var username = authPort.extractUsername();
        userUpdateService.requestPasswordChange(username, passwordChangeDTO.currentPassword(), passwordChangeDTO.newPassword(), passwordChangeDTO.confirmPassword());
        return ResponseEntity.ok(new SuccessResponse());
    }

    @PutMapping("/password-change-requests")
    public ResponseEntity<HttpResponse<Void>> confirmPasswordChange(
            @RequestParam String code) {
        var username = authPort.extractUsername();
        var user = userService.getUserByUsername(username);
        emailVerificationService.confirmPasswordChange(user.id(), code);
        return ResponseEntity.ok(new SuccessResponse());
    }

    @PostMapping("/email-change-requests")
    public ResponseEntity<HttpResponse<Void>> requestEmailChange(
            @RequestBody RequestEmailChangeDTO dto) {
        var username = authPort.extractUsername();
        userUpdateService.requestEmailChange(username, dto.newEmail(), dto.currentPassword(), dto.newPassword(), dto.confirmPassword());
        return ResponseEntity.ok(new SuccessResponse());
    }

    @PutMapping("/email-change-requests")
    public ResponseEntity<HttpResponse<UserDTO>> confirmEmailChange(
            @RequestParam String code) {
        var username = authPort.extractUsername();
        var user = userService.getUserByUsername(username);
        var updatedUser = emailVerificationService.confirmEmailChange(user.id(), code);
        return ResponseEntity.ok(new ItemResponse<>(UserDTOMapper.toDTO(updatedUser)));
    }

    @PostMapping("/email-confirmations")
    public ResponseEntity<HttpResponse<Void>> resendConfirmation() {
        var username = authPort.extractUsername();
        var user = userService.getUserByUsername(username);
        if (user.emailVerified()) {
            return ResponseEntity.ok(new SuccessResponse());
        }
        emailVerificationService.sendEmailConfirmation(user.id(), user.email());
        return ResponseEntity.ok(new SuccessResponse());
    }

    @PutMapping("/password")
    public ResponseEntity<HttpResponse<Void>> setPasswordForOAuthUser(
            @RequestBody SetPasswordDTO dto) {
        var username = authPort.extractUsername();
        userUpdateService.setPasswordForOAuthUser(username, dto.newPassword(), dto.confirmPassword());
        return ResponseEntity.ok(new SuccessResponse());
    }
}