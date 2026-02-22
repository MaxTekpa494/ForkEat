package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.presentation.mapper.web.UserFormDTOMapper;
import fr.uge.forkeat.presentation.web.form.RegisterFormDTO;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.VerificationException;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserService;
import fr.uge.forkeat.service.user.UserRegistrationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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

    public AuthWebController(UserRegistrationService userRegistrationService,
                             EmailVerificationService emailVerificationService,
                             UserService userService) {
        this.userRegistrationService = userRegistrationService;
        this.emailVerificationService = emailVerificationService;
        this.userService = userService;
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
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpSession session,
            Model model,
            HttpServletResponse response) {
        if (password.length() < 8) {
            model.addAttribute("errorMessage", "le mot de passe doit faire au moins 8 charactères");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "layout/forgot-password";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Les mots de passe ne correspondent pas");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "layout/forgot-password";
        }
        try {
            var user = userService.getUserByEmail(email);
            emailVerificationService.sendPasswordChangeCode(user.id(), user.email(), password);
            session.setAttribute("forgot-password-email", email);
        } catch (ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "layout/forgot-password";
        }
        return "layout/forgot-password-code";
    }

    @PostMapping("/forgot-password-verify-code")
    public String forgotPasswordSendCode(
            @RequestParam("verificationCode") String verificationCode,
            HttpSession httpSession,
            Model model,
            HttpServletResponse response) {
        try {
            var user = userService.getUserByEmail(httpSession.getAttribute("forgot-password-email").toString());
            emailVerificationService.confirmPasswordChange(user.id(), verificationCode);
        } catch (VerificationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "layout/forgot-password-code";
        }
        return "layout/login";
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
            emailVerificationService.confirmEmail(token);
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
