package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.infrastructure.scheduler.PromotionSchedulingService;
import fr.uge.forkeat.presentation.dto.superlike.*;
import fr.uge.forkeat.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints admin pour la gestion du prix des super-likes et des promotions.
 * Sécurisés par ROLE_ADMIN via SecurityConfig (/api/admin/**).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminSuperLikeRestController {

    private final PromotionService promotionService;
    private final PromotionSchedulingService schedulingService;

    public AdminSuperLikeRestController(PromotionService promotionService,
                                        PromotionSchedulingService schedulingService) {
        this.promotionService = promotionService;
        this.schedulingService = schedulingService;
    }

    // ─── Configuration du prix de base ────────────────────────────────────

    @GetMapping("/super-like/config")
    public ResponseEntity<SuperLikeConfigDTO> getConfig() {
        return ResponseEntity.ok(SuperLikeConfigDTO.from(promotionService.getConfig()));
    }

    @PutMapping("/super-like/config")
    public ResponseEntity<SuperLikeConfigDTO> updateConfig(@RequestBody @Valid UpdateSuperLikeConfigDTO dto) {
        var updated = promotionService.updateConfig(dto.priceCents(), dto.earningsRatio());
        return ResponseEntity.ok(SuperLikeConfigDTO.from(updated));
    }

    // ─── Gestion des promotions ───────────────────────────────────────────

    @GetMapping("/promotions")
    public ResponseEntity<List<PromotionDTO>> getAllPromotions() {
        var promotions = promotionService.findAll().stream()
                .map(PromotionDTO::from)
                .toList();
        return ResponseEntity.ok(promotions);
    }

    @GetMapping("/promotions/{id}")
    public ResponseEntity<PromotionDTO> getPromotion(@PathVariable UUID id) {
        return ResponseEntity.ok(PromotionDTO.from(promotionService.findById(id)));
    }

    @PostMapping("/promotions")
    public ResponseEntity<PromotionDTO> createPromotion(@RequestBody @Valid CreatePromotionDTO dto) {
        var created = promotionService.create(
                dto.name(), dto.startsAt(), dto.endsAt(),
                dto.priceCents(), dto.bonusEveryN()
        );
        schedulingService.onCreated(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(PromotionDTO.from(created));
    }

    @PutMapping("/promotions/{id}")
    public ResponseEntity<PromotionDTO> updatePromotion(@PathVariable UUID id,
                                                         @RequestBody @Valid UpdatePromotionDTO dto) {
        var updated = promotionService.update(
                id, dto.name(), dto.startsAt(), dto.endsAt(),
                dto.priceCents(), dto.bonusEveryN()
        );
        schedulingService.onUpdated(id, updated);
        return ResponseEntity.ok(PromotionDTO.from(updated));
    }

    @DeleteMapping("/promotions/{id}")
    public ResponseEntity<Void> cancelPromotion(@PathVariable UUID id) {
        promotionService.cancel(id);
        schedulingService.onCancelled(id);
        return ResponseEntity.noContent().build();
    }
}
