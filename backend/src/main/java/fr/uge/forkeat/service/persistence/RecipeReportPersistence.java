package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.recipe.RecipeReport;

import java.util.List;
import java.util.UUID;

public interface RecipeReportPersistence {

    RecipeReport save(RecipeReport report);

    List<RecipeReport> findByRecipeId(UUID recipeId);

    List<RecipeReport> findByStatus(ReportStatus status);

    boolean existsByRecipeIdAndReporterId(UUID recipeId, UUID reporterId);
}