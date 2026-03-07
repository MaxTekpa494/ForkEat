package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.superlike.PromotionDTO;
import fr.uge.forkeat.presentation.dto.superlike.SuperLikeHistoryDTO;
import fr.uge.forkeat.service.PromotionService;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PromotionRestController {

    private final PromotionService promotionService;
    private final AuthenticationPort authPort;
    private final UserService userService;

    public PromotionRestController(PromotionService promotionService,
                                   AuthenticationPort authPort,
                                   UserService userService) {
        this.promotionService = promotionService;
        this.authPort = authPort;
        this.userService = userService;
    }

    /** Promotion actuellement en cours (publique). */
    @GetMapping("/api/promotions/active")
    public ResponseEntity<PromotionDTO> getActivePromotion() {
        return promotionService.findActive()
                .map(PromotionDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /** Promotions à venir (publique). */
    @GetMapping("/api/promotions/upcoming")
    public ResponseEntity<List<PromotionDTO>> getUpcomingPromotions() {
        var upcoming = promotionService.findUpcoming().stream()
                .map(PromotionDTO::from)
                .toList();
        return ResponseEntity.ok(upcoming);
    }

    /**
     * Historique des super-likes de l'utilisateur connecté avec les promotions appliquées.
     * Accessible aux utilisateurs authentifiés pour leur reporting financier personnel.
     */
    @GetMapping("/api/wallet/super-likes")
    public ResponseEntity<List<SuperLikeHistoryDTO>> getSuperLikeHistory() {
        var user = userService.getUserByUsername(authPort.extractUsername());
        var history = promotionService.findSuperLikeHistory(user.id()).stream()
                .map(h -> new SuperLikeHistoryDTO(
                        h.id(),
                        h.recipeId(),
                        h.recipeTitle(),
                        h.amountCents(),
                        h.isBonusFree(),
                        h.promotionName(),
                        h.createdAt()
                ))
                .toList();
        return ResponseEntity.ok(history);
    }
}
