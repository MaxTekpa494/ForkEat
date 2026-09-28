package fr.uge.forkeat.presentation.dto.smartsearch;

import fr.uge.forkeat.service.model.smartsearch.SmartSearchConfig;

import java.time.Instant;
import java.util.UUID;

public record SmartSearchConfigDTO(UUID id, int topK, long cost, Instant updatedAt) {
    public static SmartSearchConfigDTO from(SmartSearchConfig config) {
        return new SmartSearchConfigDTO(config.id(), config.topK(), config.cost(), config.updatedAt());
    }
}
