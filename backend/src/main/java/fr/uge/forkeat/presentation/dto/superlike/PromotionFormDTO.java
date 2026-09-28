package fr.uge.forkeat.presentation.dto.superlike;

/**
 * DTO de formulaire web pour la création et la modification de promotions.
 * Les dates sont des String car les inputs datetime-local soumettent des chaînes.
 */
public record PromotionFormDTO(
        String name,
        String startsAt,
        String endsAt,
        Long priceCents,
        Integer bonusEveryN
) {}
