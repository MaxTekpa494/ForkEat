package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.RecipeAlreadyReportedException;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.recipe.CreateRecipeReport;
import fr.uge.forkeat.service.model.recipe.RecipeReport;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.persistence.RecipeReportPersistence;
import fr.uge.forkeat.service.port.UserIdentityPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RecipeReportService {

    private final RecipeReportPersistence recipeReportPersistence;
    private final RecipePersistence recipePersistence;
    private final UserIdentityPort userIdentityPort;

    public RecipeReportService(RecipeReportPersistence recipeReportPersistence,
                               RecipePersistence recipePersistence,
                               UserIdentityPort userIdentityPort) {
        this.recipeReportPersistence = recipeReportPersistence;
        this.recipePersistence = recipePersistence;
        this.userIdentityPort = userIdentityPort;
    }

    @Transactional
    public RecipeReport reportRecipe(CreateRecipeReport command) {
        Objects.requireNonNull(command);
        if (!recipePersistence.existRecipe(command.recipeId())) {
            throw new RecipeNotFoundException(command.recipeId());
        }
        var reporterId = userIdentityPort.findIdByUsernameOrThrow(command.reporterUsername());
        if (recipeReportPersistence.existsByRecipeIdAndReporterId(command.recipeId(), reporterId)) {
            throw new RecipeAlreadyReportedException(command.recipeId(), reporterId);
        }
        var report = new RecipeReport(
                UUID.randomUUID(),
                command.recipeId(),
                reporterId,
                command.reportType(),
                ReportStatus.PENDING,
                command.justification(),
                null,
                null,
                null
        );
        return recipeReportPersistence.save(report);
    }

    public List<RecipeReport> findByStatus(ReportStatus status) {
        Objects.requireNonNull(status);
        return recipeReportPersistence.findByStatus(status);
    }

    public List<RecipeReport> findByRecipeId(UUID recipeId) {
        Objects.requireNonNull(recipeId);
        if (!recipePersistence.existRecipe(recipeId)) {
            throw new RecipeNotFoundException(recipeId);
        }
        return recipeReportPersistence.findByRecipeId(recipeId);
    }
}