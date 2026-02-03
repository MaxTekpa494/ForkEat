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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Objects;

@Controller
public class ProfileController {

	private final AuthenticationPort authPort;
	private final UserQueryService userQueryService;
	private final UserUpdateService userUpdateService;

	ProfileController(AuthenticationPort authPort, UserQueryService userQueryService,
			UserUpdateService userUpdateService) {
		this.authPort = Objects.requireNonNull(authPort);
		this.userQueryService = Objects.requireNonNull(userQueryService);
		this.userUpdateService = Objects.requireNonNull(userUpdateService);
	}

	@GetMapping("/profile")
	public String profile(Authentication authentication, Model model) throws ResourceNotFoundException {
		var username = authPort.extractUsername(authentication);
		var user = userQueryService.getUserByUsername(username);

		model.addAttribute("user", user);
		model.addAttribute("pageTitle", "Mon Profil - ForkEat");
		return "dashboard/profile";
	}

	@PostMapping("/profile/update")
	public String updateProfile(Authentication authentication, @RequestParam String firstName,
			@RequestParam String lastName, @RequestParam String username, RedirectAttributes redirectAttributes,
			Model model) throws ResourceNotFoundException {
		var currentUsername = authPort.extractUsername(authentication);
		var currentUser = userQueryService.getUserByUsername(currentUsername);

		try {
			var updatedUser = userUpdateService.updateProfile(currentUser.id(), firstName, lastName, username);

			authPort.refreshAuthentication(updatedUser);

			redirectAttributes.addFlashAttribute("success", "Profil mis à jour avec succès !");
			return "redirect:/profile";

		} catch (IllegalArgumentException e) {
			model.addAttribute("user", currentUser);
			model.addAttribute("pageTitle", "Mon Profil - ForkEat");
			model.addAttribute("error", e.getMessage());
			return "dashboard/profile";
		} catch (ResourceNotFoundException e) {
			throw new RuntimeException("Erreur critique : Utilisateur perdu pendant la mise à jour", e);
		}
	}

	@PostMapping("/profile/update-password")
	public String updatePassword(Authentication authentication, @RequestParam String currentPassword,
			@RequestParam String newPassword, @RequestParam String confirmPassword,
			RedirectAttributes redirectAttributes, Model model) throws ResourceNotFoundException {
		var username = authPort.extractUsername(authentication);
		var currentUser = userQueryService.getUserByUsername(username);

		try {
			if (!newPassword.equals(confirmPassword)) {
				throw new IllegalArgumentException("Les mots de passe ne correspondent pas");
			}

			userUpdateService.updatePassword(currentUser.id(), currentPassword, newPassword);

			redirectAttributes.addFlashAttribute("success", "Mot de passe modifié avec succès !");
			return "redirect:/profile";

		} catch (IllegalArgumentException | ResourceNotFoundException e) {
			model.addAttribute("user", currentUser);
			model.addAttribute("pageTitle", "Mon Profil - ForkEat");
			model.addAttribute("error", e.getMessage());
			return "dashboard/profile";
		}
	}

	@PostMapping("/profile/update-email")
	public String updateEmail(Authentication authentication, @RequestParam String newEmail,
			@RequestParam String currentPassword, RedirectAttributes redirectAttributes) {
		try {
			var username = authPort.extractUsername(authentication);
			var user = userQueryService.getUserByUsername(username);

			userUpdateService.updateEmail(user.id(), newEmail, currentPassword);

			redirectAttributes.addFlashAttribute("success", "Email modifié avec succès ! Veuillez vous reconnecter.");
			return "redirect:/logout";

		} catch (IllegalArgumentException | ResourceNotFoundException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/profile";
		}
	}

}
