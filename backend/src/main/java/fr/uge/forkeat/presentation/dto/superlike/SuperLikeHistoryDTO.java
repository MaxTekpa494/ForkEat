package fr.uge.forkeat.presentation.dto.superlike;

import java.time.Instant;
import java.util.UUID;

/**
 * Représente un super-like dans l'historique financier de l'utilisateur.
 * Contient la promotion appliquée (si applicable) pour un reporting non contestable.
 */
public record SuperLikeHistoryDTO(
        UUID id,
        UUID recipeId,
        String recipeTitle,
        long amountCents,
        boolean isBonusFree,
        String promotionName,   // null si aucune promo active au moment du super-like
        Instant createdAt
) {}
