package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.smartsearch.SmartSearchConfigDTO;
import fr.uge.forkeat.presentation.dto.smartsearch.UpdateSmartSearchConfigDTO;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.service.SmartSearchConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/smart-search")
public class AdminSmartSearchConfigRestController {

    private final SmartSearchConfigService configService;

    public AdminSmartSearchConfigRestController(SmartSearchConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/config")
    public ResponseEntity<HttpResponse<SmartSearchConfigDTO>> getConfig() {
        return ResponseEntity.ok(new ItemResponse<>(SmartSearchConfigDTO.from(configService.getConfig())));
    }

    @PutMapping("/config")
    public ResponseEntity<HttpResponse<SmartSearchConfigDTO>> updateConfig(@RequestBody @Valid UpdateSmartSearchConfigDTO dto) {
        var updated = configService.updateConfig(dto.topK(), dto.cost());
        return ResponseEntity.ok(new ItemResponse<>(SmartSearchConfigDTO.from(updated)));
    }
}
