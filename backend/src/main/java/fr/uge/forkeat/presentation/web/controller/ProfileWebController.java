package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.presentation.dto.user.PasswordChangeDTO;
import fr.uge.forkeat.presentation.dto.user.UserUpdateProfileDTO;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserQueryService;
import fr.uge.forkeat.service.user.UserUpdateService;
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

  ProfileWebController(AuthenticationPort authPort, UserQueryService userQueryService,
                       UserUpdateService userUpdateService,
                       EmailVerificationService emailVerificationService) {
    this.authPort = Objects.requireNonNull(authPort);
    this.userQueryService = Objects.requireNonNull(userQueryService);
    this.userUpdateService = Objects.requireNonNull(userUpdateService);
    this.emailVerificationService = Objects.requireNonNull(emailVerificationService);
  }

  @GetMapping("/profile")
  public String profile(Model model) throws ResourceNotFoundException {
    var username = authPort.extractUsername();
    var user = userQueryService.getUserByUsername(username);

    model.addAttribute("user", user);
    model.addAttribute("pageTitle", "Mon Profil - ForkEat"); // Je pense pas que c le bon endroit
                                                                                     // c plutot le front ça non ?
    return "dashboard/profile";
  }

  @PostMapping("/profile/update")
  public String updateProfile(UserUpdateProfileDTO userUpdateProfile,
                              RedirectAttributes redirectAttributes) throws ResourceNotFoundException { // Potentiel ObjectRequireNonNull

    if(userUpdateProfile.firstName().isEmpty() || userUpdateProfile.lastName().isEmpty() || userUpdateProfile.username().isEmpty()){
      redirectAttributes.addFlashAttribute("error", "Veuillez remplir tous les champs");
      return "redirect:/profile";
    }

    var currentUsername = authPort.extractUsername();

    var updatedUser = userUpdateService.updateProfile(currentUsername, userUpdateProfile.username(),
                                                                       userUpdateProfile.firstName(),
                                                                        userUpdateProfile.lastName());
    authPort.refreshAuthentication(updatedUser); // C'est obligatoire en cas de changement de username
    redirectAttributes.addFlashAttribute("success", "Profil mis à jour avec succès !");
    return "redirect:/profile";
  }

  @PostMapping("/profile/request-password-change")
  public String requestPasswordChange(PasswordChangeDTO passwordChangeDTO,
                                      RedirectAttributes redirectAttributes) {

    var username = authPort.extractUsername();

    if (!passwordChangeDTO.newPassword().equals(passwordChangeDTO.confirmPassword())) {
      redirectAttributes.addFlashAttribute("error", "Les mots de passe ne correspondent pas");
      return "redirect:/profile";
    }

    userUpdateService.requestPasswordChange(username, passwordChangeDTO.currentPassword(), passwordChangeDTO.newPassword());

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
  public String confirmPasswordChange(@RequestParam String code,
                                      RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();
    var user = userQueryService.getUserByUsername(username);

    emailVerificationService.confirmPasswordChange(user.id(), code);

    redirectAttributes.addFlashAttribute("success", "Mot de passe modifié avec succès !");
    return "redirect:/profile";
  }

  @PostMapping("/profile/request-email-change")
  public String requestEmailChange(@RequestParam String newEmail,
                                   PasswordChangeDTO passwordChangeDTO,
                                   RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();
    var user = userQueryService.getUserByUsername(username);

    if (user.authMode() == AuthMode.GOOGLE) {
      if (passwordChangeDTO.newPassword().length() < 8) {
        redirectAttributes.addFlashAttribute("error",
                "Vous devez définir un mot de passe (min 8 caractères) pour changer votre email.");
        return "redirect:/profile";
      }
      if (!passwordChangeDTO.newPassword().equals(passwordChangeDTO.confirmPassword())) {
        redirectAttributes.addFlashAttribute("error", "Les mots de passe ne correspondent pas");
        return "redirect:/profile";
      }
    }

    userUpdateService.requestEmailChange(username, newEmail, passwordChangeDTO.currentPassword(), passwordChangeDTO.newPassword());

    redirectAttributes.addFlashAttribute("actionType", "EMAIL_CHANGE");
    redirectAttributes.addFlashAttribute("infoMessage",
            "Un code de vérification a été envoyé à votre adresse email actuelle.");
    return "redirect:/profile/confirm-action";
  }

  @PostMapping("/profile/confirm-email-change")
  public String confirmEmailChange(@RequestParam String code,
                                   RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();
    var user = userQueryService.getUserByUsername(username);

    var updatedUser = emailVerificationService.confirmEmailChange(user.id(), code);

    authPort.refreshAuthentication(updatedUser);
    redirectAttributes.addFlashAttribute("success", "Email mis à jour avec succès !");
    return "redirect:/profile";
  }

  @PostMapping("/profile/resend-confirmation")
  public String resendConfirmation(RedirectAttributes redirectAttributes) {
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
