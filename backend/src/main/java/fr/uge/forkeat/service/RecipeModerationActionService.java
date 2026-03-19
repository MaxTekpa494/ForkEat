package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.ModeratorIsAuthorException;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.recipe.*;
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
    private final RecipeService recipeService;

    public RecipeModerationActionService(RecipeModerationActionPersistence moderationActionPersistence,
                                         RecipePersistence recipePersistence,
                                         RecipeReportPersistence recipeReportPersistence,
                                         UserIdentityPort userIdentityPort,
                                         RecipeService recipeService) {
        this.moderationActionPersistence = moderationActionPersistence;
        this.recipePersistence = recipePersistence;
        this.recipeReportPersistence = recipeReportPersistence;
        this.userIdentityPort = userIdentityPort;
        this.recipeService = recipeService;
    }

    @Transactional
    public RecipeModerationAction moderateRecipe(CreateRecipeModerationAction command) {
        Objects.requireNonNull(command);
        if (!recipePersistence.existRecipe(command.recipeId())) {
            throw new RecipeNotFoundException(command.recipeId());
        }
        if(command.relatedReportId() != null && !recipeReportPersistence.existsById(command.relatedReportId())) {
            throw new ResourceNotFoundException("Related report id not found : " + command.relatedReportId());
        }
        var moderatorId = userIdentityPort.findIdByUsernameOrThrow(command.moderatorUsername());
        if (recipePersistence.isAuthor(command.recipeId(), moderatorId)) {
            throw new ModeratorIsAuthorException();
        }

        var moderationAction = new RecipeModerationAction(
                UUID.randomUUID(),
                command.recipeId(),
                moderatorId,
                command.moderationActionType(),
                command.justification(),
                null,
                null,
                command.relatedReportId()
        );
        var moderationActionRow = moderationActionPersistence.save(moderationAction);
        // Si c'est un signalement alors l'acceptation de la moderation action signifie que le signalement est valide -> refus de la recette
        // Et inversement si c'est pas un signalement (relatedReportId == null)
        RecipeStatus recipeStatus;
        if(command.relatedReportId() == null) {
            recipeStatus = command.moderationActionType() == RecipeModerationActionType.APPROVED ? RecipeStatus.PUBLISHED : RecipeStatus.REJECTED;
        } else {
            var reportStatus = command.moderationActionType() == RecipeModerationActionType.APPROVED ? ReportStatus.VALIDATED : ReportStatus.DISMISSED;
            recipeReportPersistence.updateStatus(command.relatedReportId(), moderatorId, reportStatus);

            recipeStatus = command.moderationActionType() == RecipeModerationActionType.APPROVED ? RecipeStatus.REJECTED : RecipeStatus.PUBLISHED;
        }
        recipePersistence.updateStatus(command.recipeId(), recipeStatus);
        return moderationActionRow;
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
