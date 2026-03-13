package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.RecipeModerationActionEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeModerationActionEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeModerationActionRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeReportRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.recipe.RecipeModerationAction;
import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
import fr.uge.forkeat.service.persistence.RecipeModerationActionPersistence;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static fr.uge.forkeat.infrastructure.persistence.mapper.RecipeModerationActionEntityMapper.toDomain;
import static fr.uge.forkeat.infrastructure.persistence.mapper.RecipeReportEntityMapper.toEntity;

@Component
public class RecipeModerationActionPersistenceAdapter implements RecipeModerationActionPersistence {
  private final RecipeModerationActionRepository recipeModerationActionRepository;
  private final RecipeRepository recipeRepository;
  private final UserRepository userRepository;
  private final RecipeReportRepository recipeReportRepository;

  public RecipeModerationActionPersistenceAdapter(RecipeModerationActionRepository recipeModerationActionRepository,
                                                  RecipeRepository recipeRepository,
                                                  UserRepository userRepository,
                                                  RecipeReportRepository recipeReportRepository) {
    this.recipeModerationActionRepository = recipeModerationActionRepository;
    this.recipeRepository = recipeRepository;
    this.userRepository = userRepository;
    this.recipeReportRepository = recipeReportRepository;
  }

  @Override
  public RecipeModerationAction save(RecipeModerationAction action) {
    Objects.requireNonNull(action);

    var recipe = recipeRepository.getReferenceById(action.recipeId());
    var moderator = userRepository.getReferenceById(action.moderatorId());
    var report = action.relatedReportId() != null
            ? recipeReportRepository.getReferenceById(action.relatedReportId())
            : null;

    var entity = RecipeModerationActionEntityMapper.toEntity(action, recipe, moderator, report);
    var saved = recipeModerationActionRepository.save(entity);
    return RecipeModerationActionEntityMapper.toDomain(saved);
  }

  @Override
  public List<RecipeModerationAction> findByRecipeId(UUID recipeId) {
    Objects.requireNonNull(recipeId);
    return recipeModerationActionRepository.findByRecipeId(recipeId).stream()
            .map(RecipeModerationActionEntityMapper::toDomain)
            .toList();
  }

  @Override
  public List<RecipeModerationAction> findByActionType(RecipeModerationActionType actionType) {
    return recipeModerationActionRepository.findByModerationActionType(actionType).stream()
            .map(RecipeModerationActionEntityMapper::toDomain)
            .toList();
  }
}

