package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.recipe.CreateRecipeModerationAction;
import fr.uge.forkeat.service.model.recipe.RecipeModerationAction;
import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
import fr.uge.forkeat.service.persistence.RecipeModerationActionPersistence;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.persistence.RecipeReportPersistence;
import fr.uge.forkeat.service.port.UserIdentityPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class RecipeModerationActionService {
    private final RecipeModerationActionPersistence moderationActionPersistence;
    private final RecipePersistence recipePersistence;
    private final RecipeReportPersistence recipeReportPersistence;
    private final UserIdentityPort userIdentityPort;

    public RecipeModerationActionService(RecipeModerationActionPersistence moderationActionPersistence,
                                         RecipePersistence recipePersistence,
                                         RecipeReportPersistence recipeReportPersistence,
                                         UserIdentityPort userIdentityPort) {
        this.moderationActionPersistence = moderationActionPersistence;
        this.recipePersistence = recipePersistence;
        this.recipeReportPersistence = recipeReportPersistence;
        this.userIdentityPort = userIdentityPort;
    }

    @Transactional
    public RecipeModerationAction moderateRecipe(CreateRecipeModerationAction command) {
        Objects.requireNonNull(command);
        if (!recipePersistence.existRecipe(command.recipeId())) {
            throw new RecipeNotFoundException(command.recipeId());
        }
        var moderatorId = userIdentityPort.findIdByUsernameOrThrow(command.moderatorUsername());
        var moderationAction = new RecipeModerationAction(
                UUID.randomUUID(),
                command.recipeId(),
                moderatorId,
                command.moderationActionType(),
                command.justification(),
                null,
                null,
                null
        );
        return moderationActionPersistence.save(moderationAction);
    }

    public List<RecipeModerationAction> findByType(RecipeModerationActionType type) {
        Objects.requireNonNull(type);
        return moderationActionPersistence.findByActionType(type);
    }

    public List<RecipeModerationAction> findByRecipeId(UUID recipeId) {
        Objects.requireNonNull(recipeId);
        if (!recipePersistence.existRecipe(recipeId)) {
            throw new RecipeNotFoundException(recipeId);
        }
        return moderationActionPersistence.findByRecipeId(recipeId);
    }
}

