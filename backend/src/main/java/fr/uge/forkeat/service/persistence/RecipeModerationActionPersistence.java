package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.recipe.RecipeModerationAction;
import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;

import java.util.List;
import java.util.UUID;

public interface RecipeModerationActionPersistence {

    RecipeModerationAction save(RecipeModerationAction action);

    List<RecipeModerationAction> findByRecipeId(UUID recipeId);

    List<RecipeModerationAction> findByActionType(RecipeModerationActionType actionType);
}