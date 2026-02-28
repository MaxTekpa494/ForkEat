package fr.uge.forkeat.presentation.dto.recipe;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecipeDetailsDTO(
        UUID id,
        String title,
        String summary,
        UUID parentId,
        //UserDTO author, pas besion d'avoir toutes infos sur l'auteur non ?
        String username, // Juste avec le username on est bon
        int preparationMinutes,
        String imageUrl,
        String status,
        List<RecipeStepDTO> steps,
        List<RecipeIngredientDTO> ingredients,
        List<AllergenDTO> allergens,
        List<String> dietaries,
        Instant createdAt,
        Instant updatedAt,
        long nbLike,
        boolean hasLiked,
        long nbSuperLike,
        boolean hasSuperLiked
) {

    public RecipeDTO toRecipeDTO(){
        return new RecipeDTO(
                id,
                title,
                summary,
                parentId,
                username,
                preparationMinutes,
                imageUrl,
                status,
                steps,
                ingredients,
                allergens,
                dietaries,
                createdAt,
                updatedAt
        );
    }
}
