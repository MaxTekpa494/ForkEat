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

  AccountWebController(AuthenticationPort authPort, UserService userService,
                       UserUpdateService userUpdateService,
                       EmailVerificationService emailVerificationService,
                       ProfileService profileService) {
    this.authPort = authPort;
    this.userService = userService;
    this.userUpdateService = userUpdateService;
    this.emailVerificationService = emailVerificationService;
    this.profileService = profileService;
  }

  @GetMapping()
  public String account(Model model){
    var username = authPort.extractUsername();
    var vm = profileService.getAccountDetails(username);
    model.addAttribute("vm", vm);
    return "account/index";
  }

  @PostMapping
  public String updateProfile(UserUpdateProfileDTO userUpdateProfile,
                              RedirectAttributes redirectAttributes){

    var currentUsername = authPort.extractUsername();
    var updatedUser = userUpdateService.updateProfile(currentUsername, userUpdateProfile.username(),
                                                                       userUpdateProfile.firstName(),
                                                                        userUpdateProfile.lastName());
    authPort.refreshAuthentication(updatedUser);
    redirectAttributes.addFlashAttribute("success", "Profil mis à jour avec succès !");
    return "redirect:/account";
  }

  @PostMapping("/password-change-requests")
  public String requestPasswordChange(PasswordChangeDTO passwordChangeDTO,
                                      RedirectAttributes redirectAttributes) {

    var username = authPort.extractUsername();
    userUpdateService.requestPasswordChange(username, passwordChangeDTO.currentPassword(), passwordChangeDTO.newPassword(), passwordChangeDTO.confirmPassword());

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

  @PostMapping("/password-change-requests/confirm")
  public String confirmPasswordChange(@RequestParam String code,
                                      RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();
    var user = userService.getUserByUsername(username);

    emailVerificationService.confirmPasswordChange(user.id(), code);

    redirectAttributes.addFlashAttribute("success", "Mot de passe modifié avec succès !");
    return "redirect:/account";
  }

  @PostMapping("/email-change-requests")
  public String requestEmailChange(@RequestParam String newEmail,
                                   @RequestParam(required = false) String currentPassword,
                                   @RequestParam(required = false) String newPassword,
                                   @RequestParam(required = false) String confirmPassword,
                                   RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();
    userUpdateService.requestEmailChange(username, newEmail, currentPassword, newPassword, confirmPassword);

    redirectAttributes.addFlashAttribute("actionType", "EMAIL_CHANGE");
    redirectAttributes.addFlashAttribute("infoMessage",
            "Un code de vérification a été envoyé à votre adresse email actuelle.");
    return "redirect:/account/confirm-action";
  }

  @PostMapping("/email-change-requests/confirm")
  public String confirmEmailChange(@RequestParam String code,
                                   RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();
    var user = userService.getUserByUsername(username);

    var updatedUser = emailVerificationService.confirmEmailChange(user.id(), code);

    authPort.refreshAuthentication(updatedUser);
    redirectAttributes.addFlashAttribute("success", "Email mis à jour avec succès !");
    return "redirect:/account";
  }

  @PostMapping("/email-confirmations")
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

  @PostMapping("/password")
  public String setPasswordForOAuthUser(@RequestParam String newPassword,
                                        @RequestParam String confirmPassword,
                                        RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();
    userUpdateService.setPasswordForOAuthUser(username, newPassword, confirmPassword);
    redirectAttributes.addFlashAttribute("success",
            "Mot de passe défini ! Vous pouvez maintenant vous connecter avec votre email et mot de passe.");
    return "redirect:/account";
  }
}
