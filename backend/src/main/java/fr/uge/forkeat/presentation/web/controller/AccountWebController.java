package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.presentation.dto.user.PasswordChangeDTO;
import fr.uge.forkeat.presentation.dto.user.UserUpdateProfileDTO;
import fr.uge.forkeat.service.model.PasswordValidator;
import fr.uge.forkeat.service.ProfileService;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.port.PasswordHasherPort;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserService;
import fr.uge.forkeat.service.user.UserUpdateService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountWebController {

  private final AuthenticationPort authPort;
  private final UserService userService;
  private final UserUpdateService userUpdateService;
  private final EmailVerificationService emailVerificationService;
  private final ProfileService profileService;
  private final PasswordHasherPort passwordHasherPort;

  AccountWebController(AuthenticationPort authPort, UserService userService,
                       UserUpdateService userUpdateService,
                       EmailVerificationService emailVerificationService,
                       ProfileService profileService,
                       PasswordHasherPort passwordHasherPort) {
    this.authPort = authPort;
    this.userService = userService;
    this.userUpdateService = userUpdateService;
    this.emailVerificationService = emailVerificationService;
    this.profileService = profileService;
    this.passwordHasherPort = passwordHasherPort;
  }

  @GetMapping()
  public String account(Model model) throws ResourceNotFoundException {
    var username = authPort.extractUsername();
    var vm = profileService.getAccountDetails(username);
    model.addAttribute("vm", vm);
    return "account/index";
  }

  @PostMapping("/update")
  public String updateProfile(UserUpdateProfileDTO userUpdateProfile,
                              RedirectAttributes redirectAttributes) throws ResourceNotFoundException {

    if(userUpdateProfile.firstName().isEmpty() || userUpdateProfile.lastName().isEmpty() || userUpdateProfile.username().isEmpty()){
      redirectAttributes.addFlashAttribute("error", "Veuillez remplir tous les champs");
      return "redirect:/account";
    }

    var currentUsername = authPort.extractUsername();
    var updatedUser = userUpdateService.updateProfile(currentUsername, userUpdateProfile.username(),
                                                                       userUpdateProfile.firstName(),
                                                                        userUpdateProfile.lastName());
    authPort.refreshAuthentication(updatedUser);
    redirectAttributes.addFlashAttribute("success", "Profil mis à jour avec succès !");
    return "redirect:/account";
  }

  @PostMapping("/request-password-change")
  public String requestPasswordChange(PasswordChangeDTO passwordChangeDTO,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {

    var username = authPort.extractUsername();

    if (!passwordChangeDTO.newPassword().equals(passwordChangeDTO.confirmPassword())) {
      redirectAttributes.addFlashAttribute("error", "Les mots de passe ne correspondent pas");
      return "redirect:/account";
    }

    userUpdateService.requestPasswordChange(username, passwordChangeDTO.currentPassword(), passwordChangeDTO.newPassword());

    session.setAttribute("pending-password-hash", passwordHasherPort.hash(passwordChangeDTO.newPassword()));

    redirectAttributes.addFlashAttribute("actionType", "PASSWORD_CHANGE");
    redirectAttributes.addFlashAttribute("infoMessage",
            "Un code de vérification a été envoyé à votre adresse email.");
    return "redirect:/account/confirm-action";
  }

  @GetMapping("/confirm-action")
  public String confirmActionPage(@RequestParam(required = false) String actionType, Model model) {
    if (!model.containsAttribute("actionType") && actionType != null) {
      model.addAttribute("actionType", actionType);
    }
    return "account/confirm-action";
  }

  @PostMapping("/confirm-password-change")
  public String confirmPasswordChange(@RequestParam String code,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();
    var user = userService.getUserByUsername(username);

    var hashedPassword = (String) session.getAttribute("pending-password-hash");
    if (hashedPassword == null) {
      redirectAttributes.addFlashAttribute("error", "Session expirée, veuillez recommencer.");
      return "redirect:/account";
    }

    emailVerificationService.confirmPasswordChange(user.id(), code, hashedPassword);
    session.removeAttribute("pending-password-hash");

    redirectAttributes.addFlashAttribute("success", "Mot de passe modifié avec succès !");
    return "redirect:/account";
  }

  @PostMapping("/request-email-change")
  public String requestEmailChange(@RequestParam String newEmail,
                                   PasswordChangeDTO passwordChangeDTO,
                                   RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();
    var user = userService.getUserByUsername(username);

    if (user.authMode() == AuthMode.GOOGLE) {
      if (passwordChangeDTO.newPassword().length() < 8) {
        redirectAttributes.addFlashAttribute("error",
                "Vous devez définir un mot de passe (min 8 caractères) pour changer votre email.");
        return "redirect:/account";
      }
      if (!passwordChangeDTO.newPassword().equals(passwordChangeDTO.confirmPassword())) {
        redirectAttributes.addFlashAttribute("error", "Les mots de passe ne correspondent pas");
        return "redirect:/account";
      }
    }

    userUpdateService.requestEmailChange(username, newEmail, passwordChangeDTO.currentPassword(), passwordChangeDTO.newPassword());

    redirectAttributes.addFlashAttribute("actionType", "EMAIL_CHANGE");
    redirectAttributes.addFlashAttribute("infoMessage",
            "Un code de vérification a été envoyé à votre adresse email actuelle.");
    return "redirect:/account/confirm-action";
  }

  @PostMapping("/confirm-email-change")
  public String confirmEmailChange(@RequestParam String code,
                                   RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();
    var user = userService.getUserByUsername(username);

    var updatedUser = emailVerificationService.confirmEmailChange(user.id(), code);

    authPort.refreshAuthentication(updatedUser);
    redirectAttributes.addFlashAttribute("success", "Email mis à jour avec succès !");
    return "redirect:/account";
  }

  @PostMapping("/resend-confirmation")
  public String resendConfirmation(RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();
    var user = userService.getUserByUsername(username);

    if (user.emailVerified()) {
      redirectAttributes.addFlashAttribute("success", "Votre email est déjà confirmé.");
      return "redirect:/account";
    }

    emailVerificationService.sendEmailConfirmation(user.id(), user.email());
    redirectAttributes.addFlashAttribute("success", "Un nouvel email de confirmation a été envoyé.");
    return "redirect:/account";
  }

  @PostMapping("/set-password")
  public String setPasswordForOAuthUser(@RequestParam String newPassword,
                                        @RequestParam String confirmPassword,
                                        RedirectAttributes redirectAttributes) {
    if (!newPassword.equals(confirmPassword)) {
      redirectAttributes.addFlashAttribute("error", "Les mots de passe ne correspondent pas");
      return "redirect:/account";
    }

    try {
      PasswordValidator.validate(newPassword);
    } catch (RegisterFailureException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:/account";
    }

    var username = authPort.extractUsername();
    userUpdateService.setPasswordForOAuthUser(username, newPassword);
    redirectAttributes.addFlashAttribute("success",
            "Mot de passe défini ! Vous pouvez maintenant vous connecter avec votre email et mot de passe.");
    return "redirect:/account";
  }
}
