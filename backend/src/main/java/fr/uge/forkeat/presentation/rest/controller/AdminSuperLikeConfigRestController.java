package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.superlike.SuperLikeConfigDTO;
import fr.uge.forkeat.presentation.dto.superlike.UpdateSuperLikeConfigDTO;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/super-like")
public class AdminSuperLikeConfigRestController {

    private final PromotionService promotionService;

    public AdminSuperLikeConfigRestController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping("/config")
    public ResponseEntity<HttpResponse<SuperLikeConfigDTO>> getConfig() {
        return ResponseEntity.ok(new ItemResponse<>(SuperLikeConfigDTO.from(promotionService.getConfig())));
    }

    @PutMapping("/config")
    public ResponseEntity<HttpResponse<SuperLikeConfigDTO>> updateConfig(@RequestBody @Valid UpdateSuperLikeConfigDTO dto) {
        var updated = promotionService.updateConfig(dto.priceCents(), dto.earningsRatio());
        return ResponseEntity.ok(new ItemResponse<>(SuperLikeConfigDTO.from(updated)));
    }
}
