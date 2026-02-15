package fr.uge.forkeat.presentation.dto.recipe;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RecipeWithMetaDataDTO(
        UUID id,
        String title,
        String summary,
        RecipeDTO parent,
        //UserDTO author, pas besion d'avoir toutes infos sur l'auteur non ?
        String username, // Juste avec le username on est bon
        int preparationMinutes,
        String imageUrl,
        String status,
        List<RecipeStepDTO> steps,
        List<RecipeIngredientDTO> ingredients,
        List<AllergenDTO> allergens,
        Map<String, Boolean> dietaryFlags,
        Instant createdAt,
        Instant updatedAt,
        long nbLike
) {
}
