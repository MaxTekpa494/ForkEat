package fr.uge.forkeat.presentation.rest.controller;


import fr.uge.forkeat.presentation.dto.user.ChangePasswordConfirmCodeDTO;
import fr.uge.forkeat.presentation.dto.user.ChangePasswordDTO;
import fr.uge.forkeat.service.exception.RegisterFailure;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserQueryService;
import fr.uge.forkeat.service.user.UserUpdateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class ProfileRestController {

    private final UserQueryService userQueryService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;

    public ProfileRestController(UserQueryService userQueryService, EmailVerificationService emailVerificationService,  PasswordEncoder passwordEncoder) {
        this.userQueryService = userQueryService;
        this.emailVerificationService = emailVerificationService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ChangePasswordDTO changePasswordDTO) {
        if(changePasswordDTO.password().length() < 8){
            throw new RegisterFailure("The password must have at least 8 characters");
        }
        var user = userQueryService.getUserByEmail(changePasswordDTO.email());
        emailVerificationService.sendPasswordChangeCode(user.id(), user.email(), passwordEncoder.encode(changePasswordDTO.password()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password/confirm-code")
    public ResponseEntity<?> forgotPasswordConfirmCode(@RequestBody ChangePasswordConfirmCodeDTO changePasswordConfirmCodeDTO) {
        var user = userQueryService.getUserByEmail(changePasswordConfirmCodeDTO.email());
        emailVerificationService.confirmPasswordChange(user.id(), changePasswordConfirmCodeDTO.code());
        return ResponseEntity.ok().build();
    }

}
