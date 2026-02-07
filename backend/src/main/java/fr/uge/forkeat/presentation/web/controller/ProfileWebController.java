package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserQueryService;
import fr.uge.forkeat.service.user.UserUpdateService;
import org.springframework.security.core.Authentication;
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

   ProfileWebController(AuthenticationPort authPort, UserQueryService userQueryService,
                       UserUpdateService userUpdateService) {
    this.authPort = Objects.requireNonNull(authPort);
    this.userQueryService = Objects.requireNonNull(userQueryService);
    this.userUpdateService = Objects.requireNonNull(userUpdateService);
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

  @PostMapping("/profile/update-password")
  public String updatePassword(Authentication authentication, @RequestParam String currentPassword,
                               @RequestParam String newPassword, @RequestParam String confirmPassword,
                               RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();

    if (!newPassword.equals(confirmPassword)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match");
      return "redirect:/profile";
    }

    userUpdateService.updatePassword(username, currentPassword, newPassword);

    redirectAttributes.addFlashAttribute("success", "Mot de passe modifié avec succès !");
    return "redirect:/profile"; // redirect mais il renvoie /profile
  }

  @PostMapping("/profile/update-email")
  public String updateEmail(Authentication authentication, @RequestParam String newEmail,
                            @RequestParam String currentPassword, RedirectAttributes redirectAttributes) {
    var username = authPort.extractUsername();
    var newUser = userUpdateService.updateEmail(username, newEmail, currentPassword);
    authPort.refreshAuthentication(newUser);

    redirectAttributes.addFlashAttribute("success", "Email mis à jour avec succès !");
    return "redirect:/profile";
  }

}
