package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.RecipeReportEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeReportRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.recipe.RecipeReport;
import fr.uge.forkeat.service.persistence.RecipeReportPersistence;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public final class RecipeReportPersistenceAdapter implements RecipeReportPersistence {

    private final RecipeReportRepository recipeReportRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    public RecipeReportPersistenceAdapter(RecipeReportRepository recipeReportRepository,
                                          RecipeRepository recipeRepository,
                                          UserRepository userRepository) {
        this.recipeReportRepository = recipeReportRepository;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
    }

    @Override
    public RecipeReport save(RecipeReport report) {
        Objects.requireNonNull(report);

        var recipe = recipeRepository.getReferenceById(report.recipeId());
        var reporter = report.reporterId() != null
                ? userRepository.getReferenceById(report.reporterId())
                : null;

        var entity = RecipeReportEntityMapper.toEntity(report, recipe, reporter);
        var saved = recipeReportRepository.save(entity);
        return RecipeReportEntityMapper.toDomain(saved);
    }

    @Override
    public List<RecipeReport> findByRecipeId(UUID recipeId) {
        Objects.requireNonNull(recipeId);
        return recipeReportRepository.findByRecipeId(recipeId).stream()
                .map(RecipeReportEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<RecipeReport> findByStatus(ReportStatus status) {
        Objects.requireNonNull(status);
        return recipeReportRepository.findByStatus(status).stream()
                .map(RecipeReportEntityMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(UUID recipeReportId) {
        Objects.requireNonNull(recipeReportId);
        return recipeReportRepository.existsById(recipeReportId);
    }

    @Override
    public boolean existsByRecipeIdAndReporterId(UUID recipeId, UUID reporterId) {
        Objects.requireNonNull(recipeId);
        Objects.requireNonNull(reporterId);
        return recipeReportRepository.existsByRecipeIdAndReporterId(recipeId, reporterId);
    }
}