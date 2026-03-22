package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.recipe.RecipeReport;
import fr.uge.forkeat.service.model.recipe.projection.RecipeReportDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeReportPersistence {

    RecipeReport save(RecipeReport report);

    Optional<RecipeReport> findById(UUID recipeReportId);

    List<RecipeReport> findByRecipeId(UUID recipeId);

    List<RecipeReport> findByStatus(ReportStatus status);

    PageResult<RecipeReportDetails> getReportsToModerateWithRecipeAndReporter(UUID reporterId, int size, int page);

    boolean existsById(UUID recipeReportId);

    boolean existsByRecipeIdAndReporterId(UUID recipeId, UUID reporterId);

}