package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.superlike.PromotionDTO;
import fr.uge.forkeat.presentation.dto.superlike.SuperLikeHistoryDTO;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.service.PromotionService;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/promotions")
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
    @GetMapping("/active")
    public ResponseEntity<HttpResponse<PromotionDTO>> getActivePromotion() {
        return promotionService.findActive()
                .map(PromotionDTO::from)
                .map(dto -> ResponseEntity.ok((HttpResponse<PromotionDTO>) new ItemResponse<>(dto)))
                .orElse(ResponseEntity.noContent().build());
    }

    /** Promotions à venir (publique). */
    @GetMapping("/upcoming")
    public ResponseEntity<HttpResponse<PromotionDTO>> getUpcomingPromotions() {
        var upcoming = promotionService.findUpcoming().stream()
                .map(PromotionDTO::from)
                .toList();
        return ResponseEntity.ok(new ListResponse<>(upcoming, upcoming.size()));
    }

    /**
     * Historique des super-likes de l'utilisateur connecté avec les promotions appliquées.
     * Accessible aux utilisateurs authentifiés pour leur reporting financier personnel.
     */
    @GetMapping("/super-likes")
    public ResponseEntity<HttpResponse<SuperLikeHistoryDTO>> getSuperLikeHistory() {
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
        return ResponseEntity.ok(new ListResponse<>(history, history.size()));
    }
}
