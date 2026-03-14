package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.service.PlatformWalletService;
import fr.uge.forkeat.service.RecipeModerationActionService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.model.recipe.CreateRecipeModerationAction;
import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import fr.uge.forkeat.service.model.user.UserRegister;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserRegistrationService;
import fr.uge.forkeat.service.user.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Objects;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminWebController {

    private static final int RECIPES_PAGE_SIZE = 20;

    private final UserService userQueryService;
    private final RecipeService recipeService;
    private final PlatformWalletService platformWalletService;
    private final AuthenticationPort authPort;
    private final UserRegistrationService userRegistrationService;
    private final RecipeModerationActionService recipeModerationActionService;

    public AdminWebController(UserService userQueryService,
                              RecipeService recipeService,
                              PlatformWalletService platformWalletService,
                              AuthenticationPort authPort,
                              UserRegistrationService userRegistrationService,
                              RecipeModerationActionService recipeModerationActionService) {
        this.userQueryService = Objects.requireNonNull(userQueryService);
        this.recipeService = Objects.requireNonNull(recipeService);
        this.platformWalletService = Objects.requireNonNull(platformWalletService);
        this.authPort = Objects.requireNonNull(authPort);
        this.userRegistrationService = Objects.requireNonNull(userRegistrationService);
        this.recipeModerationActionService = Objects.requireNonNull(recipeModerationActionService);
    }

    @GetMapping
    public String dashboard(Model model) {
        var admin = userQueryService.getUserByUsername(authPort.extractUsername());

        var memberCount = userQueryService.countByRole(UserRole.MEMBER);
        var moderatorCount = userQueryService.countByRole(UserRole.MODERATOR);

        var publishedCount = recipeService.countByStatus(RecipeStatus.PUBLISHED);
        var pendingCount = recipeService.countByStatus(RecipeStatus.PENDING_REVIEW);

        var benefitsWallet = platformWalletService.getWallet(PlatformWalletType.EARNINGS);
        var redistributionWallet = platformWalletService.getWallet(PlatformWalletType.REDISTRIBUTION);

        model.addAttribute("admin", admin);
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("moderatorCount", moderatorCount);
        model.addAttribute("publishedCount", publishedCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("benefitsWallet", benefitsWallet);
        model.addAttribute("redistributionWallet", redistributionWallet);
        model.addAttribute("pageTitle", "Administration - ForkEat");

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "MEMBER") String role, Model model) {
        var userRole = UserRole.valueOf(role);
        var users = userQueryService.getUsersByRole(userRole);

        model.addAttribute("users", users);
        model.addAttribute("selectedRole", userRole);
        model.addAttribute("pageTitle", "Gestion des utilisateurs - ForkEat");

        return "admin/users";
    }

    @GetMapping("/create-admin")
    public String createAdminForm(Model model) {
        model.addAttribute("pageTitle", "Créer un compte admin - ForkEat");
        return "admin/create-admin";
    }

    @PostMapping("/create-admin")
    public String createAdmin(@RequestParam String username,
                              @RequestParam String firstName,
                              @RequestParam String lastName,
                              @RequestParam String email,
                              @RequestParam String password,
                              Model model) {
        try {
            userRegistrationService.registerAdmin(new UserRegister(username, firstName, lastName, password, email));
            return "redirect:/admin/create-admin?success=true";
        } catch (RegisterFailureException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Créer un compte admin - ForkEat");
            return "admin/create-admin";
        }
    }

    @GetMapping("/wallets")
    public String wallets(Model model) {
        var benefitsWallet = platformWalletService.getWallet(PlatformWalletType.EARNINGS);
        var redistributionWallet = platformWalletService.getWallet(PlatformWalletType.REDISTRIBUTION);
        model.addAttribute("benefitsWallet", benefitsWallet);
        model.addAttribute("redistributionWallet", redistributionWallet);
        model.addAttribute("pageTitle", "Porte-monnaies - Administration");
        return "admin/wallets";
    }

    @GetMapping("/recipes")
    public String publishedRecipes(@RequestParam(defaultValue = "0") int page, Model model) {
        var result = recipeService.findByStatus(RecipeStatus.PUBLISHED, RECIPES_PAGE_SIZE, page);
        var totalPages = (int) Math.ceil((double) result.total() / RECIPES_PAGE_SIZE);

        model.addAttribute("recipes", result.items());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", result.total());
        model.addAttribute("pageTitle", "Recettes publiées - Administration");
        return "admin/recipes-published";
    }

    @GetMapping("/recipes/pending")
    public String pendingRecipes(Model model) {
        var recipes = recipeService.findByStatus(RecipeStatus.PENDING_REVIEW);
        model.addAttribute("recipes", recipes);
        model.addAttribute("pageTitle", "Recettes en attente - Administration");
        return "admin/recipes-pending";
    }

    @PostMapping("/recipes/{id}/validate")
    public String validateRecipe(@PathVariable UUID id) {
        recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(id, authPort.extractUsername(), RecipeModerationActionType.APPROVED, ""));
        recipeService.updateStatus(id, RecipeStatus.PUBLISHED);
        return "redirect:/admin/recipes/pending";
    }

    @PostMapping("/recipes/{id}/reject")
    public String rejectRecipe(@PathVariable UUID id, @RequestParam("justification") String justification) {
        recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(id, authPort.extractUsername(), RecipeModerationActionType.REJECTED, justification));
        recipeService.updateStatus(id, RecipeStatus.REJECTED);
        return "redirect:/admin/recipes/pending";
    }
}
