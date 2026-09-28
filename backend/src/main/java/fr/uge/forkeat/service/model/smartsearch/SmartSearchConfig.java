package fr.uge.forkeat.service.model.smartsearch;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SmartSearchConfig(UUID id, int topK, long cost, Instant updatedAt) {
    public SmartSearchConfig {
        Objects.requireNonNull(id);
        Objects.requireNonNull(updatedAt);
        if (topK <= 0) throw new IllegalArgumentException("topK must be > 0");
        if (cost <= 0) throw new IllegalArgumentException("cost must be > 0");
    }
}
