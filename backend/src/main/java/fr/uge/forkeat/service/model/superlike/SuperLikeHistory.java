package fr.uge.forkeat.service.model.superlike;

import java.time.Instant;
import java.util.UUID;

/**
 * Vue d'un super-like avec les informations de promotion et de recette,
 * destinée au reporting financier de l'utilisateur.
 */
public record SuperLikeHistory(
        UUID id,
        UUID recipeId,
        String recipeTitle,
        long amountCents,
        boolean isBonusFree,
        String promotionName,   // null si pas de promotion active à ce moment
        Instant createdAt
) {}
