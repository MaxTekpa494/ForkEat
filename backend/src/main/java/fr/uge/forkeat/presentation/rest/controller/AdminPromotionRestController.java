package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.superlike.CreatePromotionDTO;
import fr.uge.forkeat.presentation.dto.superlike.PromotionDTO;
import fr.uge.forkeat.presentation.dto.superlike.UpdatePromotionDTO;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.service.PromotionService;
import fr.uge.forkeat.service.port.PromotionSchedulingPort;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/promotions")
public class AdminPromotionRestController {

    private final PromotionService promotionService;
    private final PromotionSchedulingPort schedulingService;

    public AdminPromotionRestController(PromotionService promotionService,
                                        PromotionSchedulingPort schedulingService) {
        this.promotionService = promotionService;
        this.schedulingService = schedulingService;
    }

    @GetMapping
    public ResponseEntity<HttpResponse<PromotionDTO>> getAllPromotions() {
        var promotions = promotionService.findAll().stream()
                .map(PromotionDTO::from)
                .toList();
        return ResponseEntity.ok(new ListResponse<>(promotions, promotions.size()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HttpResponse<PromotionDTO>> getPromotion(@PathVariable UUID id) {
        return ResponseEntity.ok(new ItemResponse<>(PromotionDTO.from(promotionService.findById(id))));
    }

    @PostMapping
    public ResponseEntity<HttpResponse<PromotionDTO>> createPromotion(@RequestBody @Valid CreatePromotionDTO dto) {
        var created = promotionService.create(
                dto.name(), dto.startsAt(), dto.endsAt(),
                dto.priceCents(), dto.bonusEveryN()
        );
        schedulingService.onCreated(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ItemResponse<>(PromotionDTO.from(created)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HttpResponse<PromotionDTO>> updatePromotion(@PathVariable UUID id,
                                                                      @RequestBody @Valid UpdatePromotionDTO dto) {
        var updated = promotionService.update(
                id, dto.name(), dto.startsAt(), dto.endsAt(),
                dto.priceCents(), dto.bonusEveryN()
        );
        schedulingService.onUpdated(id, updated);
        return ResponseEntity.ok(new ItemResponse<>(PromotionDTO.from(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelPromotion(@PathVariable UUID id) {
        promotionService.cancel(id);
        schedulingService.onCancelled(id);
        return ResponseEntity.noContent().build();
    }
}
