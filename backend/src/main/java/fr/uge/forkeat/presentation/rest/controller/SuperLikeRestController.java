package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.superlike.SuperLikeHistoryDTO;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.service.PromotionService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/super-likes")
public class SuperLikeRestController {

    private final RecipeService recipeService;
    private final PromotionService promotionService;
    private final AuthenticationPort authPort;
    private final UserService userService;

    public SuperLikeRestController(RecipeService recipeService,
                                   PromotionService promotionService,
                                   AuthenticationPort authPort,
                                   UserService userService) {
        this.recipeService = recipeService;
        this.promotionService = promotionService;
        this.authPort = authPort;
        this.userService = userService;
    }

    @PostMapping("/{recipeId}")
    public ResponseEntity<?> superLikeRecipe(@PathVariable UUID recipeId) {
        var user = userService.getUserByUsername(authPort.extractUsername());
        recipeService.superLikeRecipe(user.id(), recipeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public ResponseEntity<HttpResponse<SuperLikeHistoryDTO>> getSuperLikeHistory() {
        var user = userService.getUserByUsername(authPort.extractUsername());
        var history = promotionService.findSuperLikeHistory(user.id()).stream()
                .map(histo -> new SuperLikeHistoryDTO(
                        histo.id(),
                        histo.recipeId(),
                        histo.recipeTitle(),
                        histo.amountCents(),
                        histo.isBonusFree(),
                        histo.promotionName(),
                        histo.createdAt()
                ))
                .toList();
        return ResponseEntity.ok(new ListResponse<>(history, history.size()));
    }
}
