package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.recipe.RecipeModerationAction;
import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
import fr.uge.forkeat.service.model.user.UserModerationAction;
import fr.uge.forkeat.service.model.user.UserModerationActionType;

import java.util.List;
import java.util.UUID;

public interface UserModerationActionPersistence {

    UserModerationAction save(UserModerationAction action);

    List<UserModerationAction> findByUserId(UUID userId);

    List<UserModerationAction> findByActionType(UserModerationActionType actionType);
}