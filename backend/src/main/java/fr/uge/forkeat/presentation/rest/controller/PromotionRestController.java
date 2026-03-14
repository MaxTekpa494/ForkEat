package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.superlike.PromotionDTO;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.service.PromotionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/promotions")
public class PromotionRestController {

    private final PromotionService promotionService;

    public PromotionRestController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping("/active")
    public ResponseEntity<HttpResponse<PromotionDTO>> getActivePromotion() {
        return promotionService.findActive()
                .map(PromotionDTO::from)
                .map(dto -> ResponseEntity.ok((HttpResponse<PromotionDTO>) new ItemResponse<>(dto)))
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<HttpResponse<PromotionDTO>> getUpcomingPromotions() {
        var upcoming = promotionService.findUpcoming().stream()
                .map(PromotionDTO::from)
                .toList();
        return ResponseEntity.ok(new ListResponse<>(upcoming, upcoming.size()));
    }
}
