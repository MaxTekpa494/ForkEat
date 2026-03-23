package fr.uge.forkeat.service.model.redistribution;

import java.util.UUID;

/** Ligne du récapitulatif admin : montant total distribué par recette source et par mois. */
public record RedistributionSummary(String batchMonth, UUID sourceRecipeId,
                                    String sourceRecipeTitle, long totalCents) {}