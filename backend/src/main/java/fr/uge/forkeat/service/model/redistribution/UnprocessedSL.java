package fr.uge.forkeat.service.model.redistribution;

import java.time.Instant;
import java.util.UUID;

/** Un SuperLike non encore redistribué. */
public record UnprocessedSL(UUID superLikeId, UUID recipeId, long redistAmount, Instant date) {}