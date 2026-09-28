package fr.uge.forkeat.presentation.dto.superlike;

public record PromotionEventDTO(String type, PromotionDTO promotion) {

    public static PromotionEventDTO activated(PromotionDTO dto) {
        return new PromotionEventDTO("ACTIVATED", dto);
    }

    public static PromotionEventDTO expired(PromotionDTO dto) {
        return new PromotionEventDTO("EXPIRED", dto);
    }
}
