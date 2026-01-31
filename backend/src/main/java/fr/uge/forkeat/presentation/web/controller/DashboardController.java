package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.service.CustomUserDetailsService;
import fr.uge.forkeat.service.UserService;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DashboardController {

    private final UserService userService;
    private final WalletService walletService;
    private final CustomUserDetailsService customUserDetailsService;

    public DashboardController(UserService userService, WalletService walletService, CustomUserDetailsService customUserDetailsService) {
        this.userService = userService;
        this.walletService = walletService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal UserDetails currentUser,
            Model model
    ) throws ResourceNotFoundException {
        User user = userService.getUserByUsername(currentUser.getUsername());
        Long balance = walletService.getBalance(user.id());

        model.addAttribute("user", user);
        model.addAttribute("balance", balance);
        model.addAttribute("pageTitle", "Dashboard - ForkEat");

        // Statistiques (à implémenter selon vos besoins)
        model.addAttribute("totalRecipes", 0);
        model.addAttribute("totalLikes", 0);
        model.addAttribute("followers", 0);

        return "dashboard/index";
    }

    @GetMapping("/profile")
    public String profile(
            @AuthenticationPrincipal UserDetails currentUser,
            Model model
    ) throws ResourceNotFoundException {
        User user = userService.getUserByUsername(currentUser.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Mon Profil - ForkEat");
        return "dashboard/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String username,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        try {
            User userBefore = userService.getUserByUsername(currentUser.getUsername());

            User updatedUser = userService.updateProfile(userBefore.id(), firstName, lastName, username);

            UserDetails newUserDetails = customUserDetailsService.loadUserByUsername(updatedUser.username());

            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    newUserDetails,
                    currentUser.getPassword(), // On garde le mot de passe actuel (crypté)
                    newUserDetails.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(newAuth);


            redirectAttributes.addFlashAttribute("success", "Profil mis à jour avec succès !");
            return "redirect:/profile";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "dashboard/profile";
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Erreur critique : Utilisateur perdu pendant la mise à jour", e);
        }
    }

    @PostMapping("/profile/update-email")
    public String updateEmail(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam String newEmail,
            @RequestParam String currentPassword,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        try {
            User user = userService.getUserByUsername(currentUser.getUsername());
            userService.updateEmail(user.id(), newEmail, currentPassword);

            redirectAttributes.addFlashAttribute("success",
                    "Email modifié avec succès ! Veuillez vous reconnecter.");
            return "redirect:/logout";

        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile";
        }
    }

    @PostMapping("/profile/update-password")
    public String updatePassword(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("Les mots de passe ne correspondent pas");
            }

            User user = userService.getUserByUsername(currentUser.getUsername());
            userService.updatePassword(user.id(), currentPassword, newPassword);

            redirectAttributes.addFlashAttribute("success", "Mot de passe modifié avec succès !");
            return "redirect:/profile";

        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile";
        }
    }

}