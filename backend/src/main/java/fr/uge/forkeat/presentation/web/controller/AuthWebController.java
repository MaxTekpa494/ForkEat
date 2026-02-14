package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.presentation.web.form.RegisterFormDTO;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.VerificationException;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserQueryService;
import fr.uge.forkeat.service.user.UserRegistrationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import fr.uge.forkeat.presentation.mapper.web.UserFormDTOMapper;

import java.util.Objects;

@Controller
@RequestMapping("/auth")
public class AuthWebController {

  private final UserRegistrationService userRegistrationService;
  private final UserQueryService userQueryService;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationService emailVerificationService;

  public AuthWebController(UserRegistrationService userRegistrationService,
                           EmailVerificationService emailVerificationService,
                           UserQueryService userQueryService,
                           PasswordEncoder passwordEncoder) {
    this.userRegistrationService = Objects.requireNonNull(userRegistrationService);
    this.emailVerificationService = Objects.requireNonNull(emailVerificationService);
    this.userQueryService = Objects.requireNonNull(userQueryService);
    this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
  }

  @GetMapping("/login")
  public String loginPage() {
    if (isAuthenticated()) {
      return "redirect:/dashboard";
    }
    return "layout/login";
  }

    @GetMapping("/forgot-password")
    public String forgotPassword(RedirectAttributes redirectAttributes) {
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
      if(password.length() < 8) {
          model.addAttribute("errorMessage", "le mot de passe doit faire au moins 8 charactères");
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          return "layout/forgot-password";
      }
        try{
            var user = userQueryService.getUserByEmail(email);
            emailVerificationService.sendPasswordChangeCode(user.id(), user.email(), passwordEncoder.encode(password));
            session.setAttribute("forgot-password-email", email);
        }catch(ResourceNotFoundException e){
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
            var user = userQueryService.getUserByEmail(httpSession.getAttribute("forgot-password-email").toString());
            emailVerificationService.confirmPasswordChange(user.id(), verificationCode);
        }catch(VerificationException e){
            model.addAttribute("errorMessage", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "layout/forgot-password-code";
        }
        return "layout/login";
    }

  @GetMapping("/register")
  public String registerPage(Model model) {
    if (isAuthenticated()) {
      return "redirect:/dashboard";
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
