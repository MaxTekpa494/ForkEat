package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.presentation.dto.redistribution.RedistributionChainEntryDTO;
import fr.uge.forkeat.presentation.dto.superlike.PromotionFormDTO;
import fr.uge.forkeat.presentation.web.dto.UserModerationRequest;
import fr.uge.forkeat.service.*;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.model.SortOrder;
import fr.uge.forkeat.service.model.recipe.CreateRecipeModerationAction;
import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.superlike.Promotion;
import fr.uge.forkeat.service.model.user.CreateUserModerationAction;
import fr.uge.forkeat.service.model.user.UserRegister;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.port.PromotionSchedulingPort;
import fr.uge.forkeat.service.user.UserRegistrationService;
import fr.uge.forkeat.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.time.temporal.ChronoUnit;

import static fr.uge.forkeat.presentation.ComputeSuspendedUntil.computeSuspendedUntil;

@Controller
@RequestMapping("/admin")
public class AdminWebController {

    private static final int RECIPES_PAGE_SIZE = 20;
    private static final int RECIPESREPORTS_PAGE_SIZE = 10;
    private static final int USERSREPORTS_PAGE_SIZE = 10;

    private final UserService userQueryService;
    private final RecipeService recipeService;
    private final PlatformWalletService platformWalletService;
    private final AuthenticationPort authPort;
    private final UserRegistrationService userRegistrationService;
    private final RecipeModerationActionService recipeModerationActionService;
    private final PromotionService promotionService;
    private final PromotionSchedulingPort schedulingService;
    private final RecipeReportService recipeReportService;
    private final UserReportService userReportService;
    private final UserModerationActionService userModerationActionService;
    private final RedistributionService redistributionService;
    private final SmartSearchConfigService smartSearchConfigService;

    public AdminWebController(UserService userQueryService,
                              RecipeService recipeService,
                              PlatformWalletService platformWalletService,
                              AuthenticationPort authPort,
                              UserRegistrationService userRegistrationService,
                              RecipeModerationActionService recipeModerationActionService,
                              PromotionService promotionService,
                              RecipeReportService recipeReportService,
                              UserReportService userReportService,
                              UserModerationActionService userModerationActionService,
                              PromotionSchedulingPort schedulingService,
                              RedistributionService redistributionService,
                              SmartSearchConfigService smartSearchConfigService) {
        this.userQueryService = userQueryService;
        this.recipeService = recipeService;
        this.platformWalletService = platformWalletService;
        this.authPort = authPort;
        this.userRegistrationService = userRegistrationService;
        this.recipeModerationActionService = recipeModerationActionService;
        this.promotionService = promotionService;
        this.schedulingService = schedulingService;
        this.recipeReportService = recipeReportService;
        this.userReportService = userReportService;
        this.userModerationActionService = userModerationActionService;
        this.redistributionService = redistributionService;
        this.smartSearchConfigService = smartSearchConfigService;
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

        var superLikeConfig = promotionService.getConfig();
        var activePromotion = promotionService.findActive();
        var smartSearchConfig = smartSearchConfigService.getConfig();

        model.addAttribute("admin", admin);
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("moderatorCount", moderatorCount);
        model.addAttribute("publishedCount", publishedCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("benefitsWallet", benefitsWallet);
        model.addAttribute("redistributionWallet", redistributionWallet);
        model.addAttribute("superLikeConfig", superLikeConfig);
        model.addAttribute("activePromotion", activePromotion.orElse(null));
        model.addAttribute("smartSearchConfig", smartSearchConfig);
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

    @PostMapping("/redistribution/trigger")
    public String triggerRedistribution(RedirectAttributes redirectAttributes) {
        var batchMonth = YearMonth.now().minusMonths(1).toString();
        redistributionService.processAllPending(batchMonth);
        redirectAttributes.addFlashAttribute("redistributionSuccess", true);
        return "redirect:/admin/wallets";
    }

    @GetMapping("/redistribution/chain")
    @ResponseBody
    public ResponseEntity<List<RedistributionChainEntryDTO>> getRedistributionChain(
            @RequestParam UUID recipeId,
            @RequestParam String month) {
        var chain = redistributionService.getRedistributionChain(recipeId, month).stream()
            .map(e -> new RedistributionChainEntryDTO(e.authorId().toString(), e.username(),
                                                      e.recipeId().toString(), e.recipeTitle(), e.amountCents()))
            .toList();
        return ResponseEntity.ok(chain);
    }

    @GetMapping("/wallets")
    public String wallets(Model model) {
        var benefitsWallet = platformWalletService.getWallet(PlatformWalletType.EARNINGS);
        var redistributionWallet = platformWalletService.getWallet(PlatformWalletType.REDISTRIBUTION);
        var transactions = platformWalletService.getTransactionHistory(SortOrder.DESC);
        var redistributionSummary = redistributionService.getRedistributionSummary();
        model.addAttribute("benefitsWallet", benefitsWallet);
        model.addAttribute("redistributionWallet", redistributionWallet);
        model.addAttribute("walletTransactions", transactions);
        model.addAttribute("redistributionSummary", redistributionSummary);
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
        recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(id, authPort.extractUsername(), RecipeModerationActionType.APPROVED, "", null));
        return "redirect:/admin/recipes/pending";
    }

    @PostMapping("/recipes/{id}/reject")
    public String rejectRecipe(@PathVariable UUID id, @RequestParam("justification") String justification) {
        recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(id, authPort.extractUsername(), RecipeModerationActionType.REJECTED, justification, null));
        return "redirect:/admin/recipes/pending";
    }

    @GetMapping("/super-like")
    public String superLikePage(Model model) {
        model.addAttribute("config", promotionService.getConfig());
        model.addAttribute("promotions", promotionService.findAll());
        model.addAttribute("pageTitle", "Super-likes & Promotions - Administration");
        return "admin/super-like";
    }

    @PostMapping("/super-like/config")
    public String updateConfig(@RequestParam long priceCents,
                               @RequestParam BigDecimal earningsRatio) {
        promotionService.updateConfig(priceCents, earningsRatio);
        return "redirect:/admin/super-like?configSuccess=true";
    }

    @PostMapping("/smart-search/config")
    public String updateSmartSearchConfig(@RequestParam int topK,
                                          @RequestParam long cost) {
        smartSearchConfigService.updateConfig(topK, cost);
        return "redirect:/admin?smartSearchConfigSuccess=true";
    }

    @GetMapping("/promotions/create")
    public String createPromotionForm(Model model) {
        addConfigAttributes(model);
        model.addAttribute("promotion", (Promotion) null);
        model.addAttribute("pageTitle", "Créer une promotion - Administration");
        return "admin/promotion-form";
    }

    @PostMapping("/promotions/create")
    public String createPromotion(@ModelAttribute PromotionFormDTO form) {
        var zone = ZoneId.systemDefault();
        var starts = LocalDateTime.parse(form.startsAt()).atZone(zone).toInstant();
        var ends = LocalDateTime.parse(form.endsAt()).atZone(zone).toInstant();
        var created = promotionService.create(form.name(), starts, ends, form.priceCents(), form.bonusEveryN());
        schedulingService.onCreated(created);
        return "redirect:/admin/super-like?promoSuccess=true";
    }

    @GetMapping("/promotions/{id}/edit")
    public String editPromotionForm(@PathVariable UUID id, Model model) {
        addConfigAttributes(model);
        model.addAttribute("promotion", promotionService.findById(id));
        model.addAttribute("pageTitle", "Modifier la promotion - Administration");
        return "admin/promotion-form";
    }

    @PostMapping("/promotions/{id}/edit")
    public String editPromotion(@PathVariable UUID id, @ModelAttribute PromotionFormDTO form) {
        var zone = ZoneId.systemDefault();
        var starts = LocalDateTime.parse(form.startsAt()).atZone(zone).toInstant();
        var ends = LocalDateTime.parse(form.endsAt()).atZone(zone).toInstant();
        var updated = promotionService.update(id, form.name(), starts, ends, form.priceCents(), form.bonusEveryN());
        schedulingService.onUpdated(id, updated);
        return "redirect:/admin/super-like?promoSuccess=true";
    }

    @PostMapping("/promotions/{id}/cancel")
    public String cancelPromotion(@PathVariable UUID id) {
        promotionService.cancel(id);
        schedulingService.onCancelled(id);
        return "redirect:/admin/super-like?promoCancelled=true";
    }

    @GetMapping("/recipes/reports")
    public String getPendingReports(@RequestParam(defaultValue = "0") int page, Model model) {
        var result = recipeReportService.getReportsToModerate(authPort.extractUsername(), RECIPESREPORTS_PAGE_SIZE, page);
        var totalPages = (int) Math.ceil((double) result.total() / RECIPESREPORTS_PAGE_SIZE);
        model.addAttribute("reports", result.items());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", result.total());
        model.addAttribute("pageTitle", "Signalements de recettes - Administration");
        return "admin/recipes-reports";
    }

    @GetMapping("/users/reports")
    public String reportedUsers(@RequestParam(defaultValue = "0") int page, Model model) {
        var result = userReportService.getReportsToModerate(authPort.extractUsername(), USERSREPORTS_PAGE_SIZE, page);
        var totalPages = (int) Math.ceil((double) result.total() / USERSREPORTS_PAGE_SIZE);
        model.addAttribute("reports", result.items());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", result.total());
        model.addAttribute("pageTitle", "Signalements d'utilisateurs - Administration");
        return "admin/users-reports";
    }

    @PostMapping("/recipes/reports/{reportId}/validate")
    public String validateRecipeReport(@PathVariable UUID reportId, @RequestParam("recipeId") UUID recipeId) {
        recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(recipeId, authPort.extractUsername(), RecipeModerationActionType.APPROVED, "", reportId));
        return "redirect:/admin/recipes/reports";
    }

    @PostMapping("/recipes/reports/{reportId}/dismiss")
    public String rejectRecipeReport(@PathVariable UUID reportId, @RequestParam("recipeId") UUID recipeId, @RequestParam("justification") String justification) {
        recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(recipeId, authPort.extractUsername(), RecipeModerationActionType.REJECTED, justification, reportId));
        return "redirect:/admin/recipes/reports";
    }

    @PostMapping("/users/reports/{reportId}/resolve")
    public String resolveUserReport(@PathVariable UUID reportId, @ModelAttribute UserModerationRequest request) {
        var suspendedUntil = computeSuspendedUntil(request.action(), request.suspensionDays(), request.suspensionHours());
        userModerationActionService.moderateUser(
                new CreateUserModerationAction(
                        request.userId(),
                        authPort.extractUsername(),
                        request.action(),
                        request.justification(),
                        suspendedUntil,
                        reportId
                )
        );
        return "redirect:/admin/users/reports";
    }

    private void addConfigAttributes(Model model) {
        var config = promotionService.getConfig();
        double ratio = config.earningsRatio().doubleValue();
        int minBonusEveryN = (int) Math.floor((1.0 - ratio) / ratio) + 1;
        model.addAttribute("config", config);
        model.addAttribute("minBonusEveryN", minBonusEveryN);
    }
}
