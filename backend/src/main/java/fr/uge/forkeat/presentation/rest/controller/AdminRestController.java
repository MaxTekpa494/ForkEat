package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.admin.AdminRecipeStatsDTO;
import fr.uge.forkeat.presentation.dto.admin.AdminUserStatsDTO;
import fr.uge.forkeat.presentation.dto.admin.PlatformWalletDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.dto.user.UserDTO;
import fr.uge.forkeat.presentation.dto.user.UserRegisterDTO;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.presentation.mapper.rest.UserDTOMapper;
import fr.uge.forkeat.presentation.response.CreatedResponse;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.service.PlatformWalletService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.user.UserRegistrationService;

import java.util.Objects;
import java.util.UUID;

import fr.uge.forkeat.service.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

    private final UserRegistrationService userRegistrationService;
    private final UserService userService;
    private final RecipeService recipeService;
    private final PlatformWalletService platformWalletService;

    public AdminRestController(UserRegistrationService userRegistrationService,
                               UserService userService,
                               RecipeService recipeService,
                               PlatformWalletService platformWalletService) {
        this.userRegistrationService = Objects.requireNonNull(userRegistrationService);
        this.userService = Objects.requireNonNull(userService);
        this.recipeService = Objects.requireNonNull(recipeService);
        this.platformWalletService = Objects.requireNonNull(platformWalletService);
    }

    @PostMapping("/register") // Pour une utilisation future
    public ResponseEntity<HttpResponse<UserDTO>> registerModerator(@RequestBody UserRegisterDTO userRegisterDTO) {
        Objects.requireNonNull(userRegisterDTO);
        var user = userRegistrationService.registerModerator(UserDTOMapper.toUserRegister(userRegisterDTO));
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedResponse<>(UserDTOMapper.toDTO(user)));
    }

    @PostMapping("/admins")
    public ResponseEntity<HttpResponse<UserDTO>> createAdmin(@RequestBody UserRegisterDTO userRegisterDTO) {
        Objects.requireNonNull(userRegisterDTO);
        var user = userRegistrationService.registerAdmin(UserDTOMapper.toUserRegister(userRegisterDTO));
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedResponse<>(UserDTOMapper.toDTO(user)));
    }

    @GetMapping("/admins")
    public ResponseEntity<HttpResponse<UserDTO>> getAdmins() {
        var admins = userService.getUsersByRole(UserRole.ADMIN)
                .stream().map(UserDTOMapper::toDTO).toList();
        return ResponseEntity.ok(new ListResponse<>(admins, admins.size()));
    }

    @GetMapping("/users")
    public ResponseEntity<HttpResponse<UserDTO>> getMembers() {
        var members = userService.getUsersByRole(UserRole.MEMBER)
                .stream().map(UserDTOMapper::toDTO).toList();
        return ResponseEntity.ok(new ListResponse<>(members, members.size()));
    }

    @GetMapping("/moderators")
    public ResponseEntity<HttpResponse<UserDTO>> getModerators() {
        var moderators = userService.getUsersByRole(UserRole.MODERATOR)
                .stream().map(UserDTOMapper::toDTO).toList();
        return ResponseEntity.ok(new ListResponse<>(moderators, moderators.size()));
    }

    @GetMapping("/stats/users")
    public ResponseEntity<AdminUserStatsDTO> getUserStats() {
        var memberCount = userService.countByRole(UserRole.MEMBER);
        var moderatorCount = userService.countByRole(UserRole.MODERATOR);
        var adminCount = userService.countByRole(UserRole.ADMIN);
        return ResponseEntity.ok(new AdminUserStatsDTO(memberCount, moderatorCount, adminCount));
    }

    @GetMapping("/stats/recipes")
    public ResponseEntity<AdminRecipeStatsDTO> getRecipeStats() {
        var published = recipeService.countByStatus(RecipeStatus.PUBLISHED);
        var pending = recipeService.countByStatus(RecipeStatus.PENDING_REVIEW);
        var draft = recipeService.countByStatus(RecipeStatus.DRAFT);
        return ResponseEntity.ok(new AdminRecipeStatsDTO(published, pending, draft));
    }

    private static final int RECIPES_PAGE_SIZE = 20;

    @GetMapping("/recipes/pending")
    public ResponseEntity<HttpResponse<RecipeDTO>> getPendingRecipes() {
        var recipes = recipeService.findByStatus(RecipeStatus.PENDING_REVIEW);
        var dtos = recipes.stream().map(RecipeDTOMapper::toDTO).toList();
        return ResponseEntity.ok(new ListResponse<>(dtos, dtos.size()));
    }

    @GetMapping("/recipes/published")
    public ResponseEntity<HttpResponse<RecipeDTO>> getPublishedRecipes(
            @RequestParam(defaultValue = "0") int page) {
        var result = recipeService.findByStatus(RecipeStatus.PUBLISHED, RECIPES_PAGE_SIZE, page);
        var dtos = result.items().stream().map(RecipeDTOMapper::toDTO).toList();
        return ResponseEntity.ok(new ListResponse<>(dtos, result.total()));
    }

    @PostMapping("/recipes/{id}/validate")
    public ResponseEntity<Void> validateRecipe(@PathVariable UUID id) {
        recipeService.updateStatus(id, RecipeStatus.PUBLISHED);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recipes/{id}/reject")
    public ResponseEntity<Void> rejectRecipe(@PathVariable UUID id) {
        recipeService.updateStatus(id, RecipeStatus.REJECTED);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/wallets/benefits")
    public ResponseEntity<PlatformWalletDTO> getBenefitsWallet() {
        var wallet = platformWalletService.getWallet(PlatformWalletType.EARNINGS);
        return ResponseEntity.ok(new PlatformWalletDTO(wallet.type().name(), wallet.balance(), wallet.updatedAt()));
    }

    @GetMapping("/wallets/redistribution")
    public ResponseEntity<PlatformWalletDTO> getRedistributionWallet() {
        var wallet = platformWalletService.getWallet(PlatformWalletType.REDISTRIBUTION);
        return ResponseEntity.ok(new PlatformWalletDTO(wallet.type().name(), wallet.balance(), wallet.updatedAt()));
    }
}
