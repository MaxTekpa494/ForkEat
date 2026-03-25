package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.presentation.mapper.web.UserFormDTOMapper;
import fr.uge.forkeat.presentation.web.form.RegisterFormDTO;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.exception.VerificationException;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserService;
import fr.uge.forkeat.service.user.UserRegistrationService;
import fr.uge.forkeat.service.user.UserUpdateService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthWebController {

    private final UserRegistrationService userRegistrationService;
    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final UserUpdateService userUpdateService;
    private final AuthenticationPort authPort;

    public AuthWebController(UserRegistrationService userRegistrationService,
                             EmailVerificationService emailVerificationService,
                             UserService userService,
                             UserUpdateService userUpdateService,
                             AuthenticationPort authPort) {
        this.userRegistrationService = userRegistrationService;
        this.emailVerificationService = emailVerificationService;
        this.userService = userService;
        this.userUpdateService = userUpdateService;
        this.authPort = authPort;
    }

    @GetMapping("/login")
    public String loginPage() {
        if (isAuthenticated()) {
            return "redirect:/profile";
        }
        return "layout/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "layout/forgot-password";
    }

    @PostMapping("/forgot-password-code")
    public String forgotPasswordCode(
            @RequestParam("email") String email,
            Model model) {
        // Always show the code page — don't reveal whether the email exists
        var userOpt = userService.findByEmail(email);
        userOpt.ifPresent(user ->
                emailVerificationService.sendPasswordChangeCode(user.id(), user.email()));
        model.addAttribute("email", email);
        return "layout/forgot-password-code";
    }

    @PostMapping("/forgot-password-verify-code")
    public String forgotPasswordVerifyCode(
            @RequestParam("email") String email,
            @RequestParam("verificationCode") String verificationCode,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        userUpdateService.confirmForgotPasswordChange(email, verificationCode, password, confirmPassword);
        redirectAttributes.addFlashAttribute("success",
                "Mot de passe réinitialisé avec succès ! Vous pouvez maintenant vous connecter.");
        return "redirect:/auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (isAuthenticated()) {
            return "redirect:/profile";
        }
        model.addAttribute("registerForm", new RegisterFormDTO());
        return "layout/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterFormDTO form, BindingResult result,
                           RedirectAttributes redirectAttributes, Model model, HttpServletResponse response) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.registerForm", "Les mots de passe ne correspondent pas");
        }
        if (result.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "layout/register";
        }
        try {
            userRegistrationService.registerUser(UserFormDTOMapper.toUserRegister(form));
        } catch (RegisterFailureException e) {
            model.addAttribute("errorMessage", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "layout/register";
        }
        redirectAttributes.addFlashAttribute("success",
                "Compte créé ! Vérifiez votre email pour confirmer votre adresse.");
        return "redirect:/auth/email-sent";
    }

    @GetMapping("/email-sent")
    public String emailSentPage() {
        return "layout/email-sent";
    }

    @GetMapping("/confirm-email")
    public String confirmEmail(@RequestParam String token, RedirectAttributes redirectAttributes) {
        try {
            var user = emailVerificationService.confirmEmail(token);
            if (isAuthenticated()) {
                authPort.refreshAuthentication(user);
                redirectAttributes.addFlashAttribute("success",
                        "Votre email a été confirmé avec succès !");
                return "redirect:/account";
            }
            redirectAttributes.addFlashAttribute("success",
                    "Votre email a été confirmé avec succès ! Vous pouvez maintenant vous connecter.");
        } catch (VerificationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/auth/login";
    }

    private boolean isAuthenticated() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || AnonymousAuthenticationToken.class.isAssignableFrom(authentication.getClass())) {
            return false;
        }
        return authentication.isAuthenticated();
    }
}