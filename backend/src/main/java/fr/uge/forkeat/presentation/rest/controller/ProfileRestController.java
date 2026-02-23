package fr.uge.forkeat.presentation.rest.controller;


import fr.uge.forkeat.presentation.dto.user.ChangePasswordConfirmCodeDTO;
import fr.uge.forkeat.presentation.dto.user.ChangePasswordDTO;
import fr.uge.forkeat.service.PasswordValidator;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class ProfileRestController {

    private final UserQueryService userQueryService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;

    public ProfileRestController(UserQueryService userQueryService, EmailVerificationService emailVerificationService, PasswordEncoder passwordEncoder) {
        this.userQueryService = userQueryService;
        this.emailVerificationService = emailVerificationService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ChangePasswordDTO changePasswordDTO) {
        // On retourne toujours 200 ici, il faut pas QUE CA DONNE UNE IDEE SI LE EMAIL EXISTE
        var userOpt = userQueryService.findByEmail(changePasswordDTO.email());
        userOpt.ifPresent(user ->
                emailVerificationService.sendPasswordChangeCode(user.id(), user.email()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password/confirm-code")
    public ResponseEntity<?> forgotPasswordConfirmCode(@RequestBody ChangePasswordConfirmCodeDTO dto) {
        PasswordValidator.validate(dto.password());
        if (!dto.password().equals(dto.confirmPassword())) {
            return ResponseEntity.badRequest().body("Les mots de passe ne correspondent pas");
        }
        var user = userQueryService.getUserByEmail(dto.email());
        emailVerificationService.confirmPasswordChange(user.id(), dto.code(), passwordEncoder.encode(dto.password()));
        return ResponseEntity.ok().build();
    }

}
