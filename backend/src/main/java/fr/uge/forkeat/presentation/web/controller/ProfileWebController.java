package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserQueryService;
import fr.uge.forkeat.service.user.UserUpdateService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
public class ProfileWebController {

  private final AuthenticationPort authPort;
  private final UserQueryService userQueryService;
  private final UserUpdateService userUpdateService;
  private final EmailVerificationService emailVerificationService;
  private final PasswordEncoder passwordEncoder;

  ProfileWebController(AuthenticationPort authPort, UserQueryService userQueryService,
                       UserUpdateService userUpdateService,
                       EmailVerificationService emailVerificationService,
                       PasswordEncoder passwordEncoder) {
    this.authPort = Objects.requireNonNull(authPort);
    this.userQueryService = Objects.requireNonNull(userQueryService);
    this.userUpdateService = Objects.requireNonNull(userUpdateService);
    this.emailVerificationService = Objects.requireNonNull(emailVerificationService);
    this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
  }

  @GetMapping("/profile")
  public String profile(Authentication authentication, Model model) throws ResourceNotFoundException {
    var username = authPort.extractUsername();
    var user = userQueryService.getUserByUsername(username);

    model.addAttribute("user", user);
    model.addAttribute("pageTitle", "Mon Profil - ForkEat");
    return "dashboard/profile";
  }

  @PostMapping("/profile/update")
  public String updateProfile(Authentication authentication,
                              @RequestParam String firstName,
                              @RequestParam String lastName,
                              @RequestParam String username,
                              RedirectAttributes redirectAttributes) throws ResourceNotFoundException {
    var currentUsername = authPort.extractUsername();

    var updatedUser = userUpdateService.updateProfile(currentUsername, username, firstName, lastName);
    authPort.refreshAuthentication(updatedUser);
    redirectAttributes.addFlashAttribute("success", "Profil mis à jour avec succès !");
    return "redirect:/profile";
  }

  @PostMapping("/profile/request-password-change")
  public String requestPasswordChange(Authentication authentication,
                                      @RequestParam String currentPassword,
                                      @RequestParam String newPassword,
                                      @RequestParam String confirmPassword,
                                      RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();

    if (!newPassword.equals(confirmPassword)) {
      redirectAttributes.addFlashAttribute("error", "Les mots de passe ne correspondent pas");
      return "redirect:/profile";
    }

    userUpdateService.requestPasswordChange(username, currentPassword, newPassword);

    redirectAttributes.addFlashAttribute("actionType", "PASSWORD_CHANGE");
    redirectAttributes.addFlashAttribute("infoMessage",
            "Un code de vérification a été envoyé à votre adresse email.");
    return "redirect:/profile/confirm-action";
  }

  @GetMapping("/profile/confirm-action")
  public String confirmActionPage(@RequestParam(required = false) String actionType, Model model) {
    if (!model.containsAttribute("actionType") && actionType != null) {
      model.addAttribute("actionType", actionType);
    }
    return "dashboard/confirm-action";
  }

  @PostMapping("/profile/confirm-password-change")
  public String confirmPasswordChange(Authentication authentication,
                                      @RequestParam String code,
                                      RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();
    var user = userQueryService.getUserByUsername(username);

    emailVerificationService.confirmPasswordChange(user.id(), code);

    redirectAttributes.addFlashAttribute("success", "Mot de passe modifié avec succès !");
    return "redirect:/profile";
  }

  @PostMapping("/profile/request-email-change")
  public String requestEmailChange(Authentication authentication,
                                   @RequestParam String newEmail,
                                   @RequestParam(required = false) String currentPassword,
                                   @RequestParam(required = false) String newPassword,
                                   @RequestParam(required = false) String confirmNewPassword,
                                   RedirectAttributes redirectAttributes,
                                   HttpSession session) {
    var username = authPort.extractUsername();
    var user = userQueryService.getUserByUsername(username);

    if (user.authMode() == AuthMode.GOOGLE) {
      if (newPassword == null || newPassword.length() < 8) {
        redirectAttributes.addFlashAttribute("error",
                "Vous devez définir un mot de passe (min 8 caractères) pour changer votre email.");
        return "redirect:/profile";
      }
      if (!newPassword.equals(confirmNewPassword)) {
        redirectAttributes.addFlashAttribute("error", "Les mots de passe ne correspondent pas");
        return "redirect:/profile";
      }
      session.setAttribute("pendingPasswordHash", passwordEncoder.encode(newPassword));
    }

    userUpdateService.requestEmailChange(username, newEmail, currentPassword);

    redirectAttributes.addFlashAttribute("actionType", "EMAIL_CHANGE");
    redirectAttributes.addFlashAttribute("infoMessage",
            "Un code de vérification a été envoyé à votre adresse email actuelle.");
    return "redirect:/profile/confirm-action";
  }

  @PostMapping("/profile/confirm-email-change")
  public String confirmEmailChange(Authentication authentication,
                                   @RequestParam String code,
                                   RedirectAttributes redirectAttributes,
                                   HttpSession session) {
    var username = authPort.extractUsername();
    var user = userQueryService.getUserByUsername(username);

    User updatedUser;
    var pendingPasswordHash = (String) session.getAttribute("pendingPasswordHash"); // Le password est stocké dans la session ?? c'est sécurisé ça ?

    if (pendingPasswordHash != null) {
      updatedUser = emailVerificationService.confirmEmailChangeWithPassword(
              user.id(), code, pendingPasswordHash);
      session.removeAttribute("pendingPasswordHash");
    } else {
      updatedUser = emailVerificationService.confirmEmailChange(user.id(), code);
    }

    authPort.refreshAuthentication(updatedUser);
    redirectAttributes.addFlashAttribute("success", "Email mis à jour avec succès !");
    return "redirect:/profile";
  }

  @PostMapping("/profile/resend-confirmation")
  public String resendConfirmation(Authentication authentication, RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();
    var user = userQueryService.getUserByUsername(username);

    if (user.emailVerified()) {
      redirectAttributes.addFlashAttribute("success", "Votre email est déjà confirmé.");
      return "redirect:/profile";
    }

    emailVerificationService.sendEmailConfirmation(user.id(), user.email());
    redirectAttributes.addFlashAttribute("success", "Un nouvel email de confirmation a été envoyé.");
    return "redirect:/profile";
  }
}
