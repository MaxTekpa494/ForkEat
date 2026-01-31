package fr.uge.forkeat.presentation.web.controller;
import fr.uge.forkeat.presentation.web.form.RegisterForm;
import fr.uge.forkeat.service.UserService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.UserRole;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        if (isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "layout/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (isAuthenticated()) {
            return "redirect:/dashboard";
        }
        model.addAttribute("registerForm", new RegisterForm());
        return "layout/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute RegisterForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (result.hasErrors()) {
            return "layout/register";
        }

        try {
            userService.registerUser(
                    form.getFirstName(),
                    form.getLastName(),
                    form.getUserName(),
                    form.getEmail(),
                    form.getPassword(),
                    UserRole.MEMBER
            );

            redirectAttributes.addFlashAttribute("success",
                    "Compte créé avec succès ! Vous pouvez maintenant vous connecter.");
            return "redirect:/login";

        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "layout/register";
        }
        catch (Exception e) {
            model.addAttribute("error", "Une erreur est survenue : " + e.getMessage());
            return "layout/register";
        }
    }

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || AnonymousAuthenticationToken.class.isAssignableFrom(authentication.getClass())) {
            return false;
        }
        return authentication.isAuthenticated();
    }
}