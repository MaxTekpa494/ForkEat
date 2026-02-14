package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.RecipeEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.AllergenRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.IngredientRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Allergen;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeIngredient;
import fr.uge.forkeat.service.model.recipe.RecipeSearchCriteria;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public final class RecipePersistenceAdapter implements RecipePersistence {

  private final RecipeRepository recipeRepository;
  private final UserRepository userRepository;
  private final AllergenRepository allergenRepository;
  private final IngredientRepository ingredientRepository;

  public RecipePersistenceAdapter(RecipeRepository recipeRepository, UserRepository userRepository,
                                  AllergenRepository allergenRepository, IngredientRepository ingredientRepository
  ) {
    this.recipeRepository = recipeRepository;
    this.userRepository = userRepository;
    this.allergenRepository = allergenRepository;
    this.ingredientRepository = ingredientRepository;
  }

  @Override
  public Optional<Recipe> findById(UUID id) {
    Objects.requireNonNull(id);
    return recipeRepository.findById(id).map(RecipeEntityMapper::toDomain);
  }

  @Override
  public List<Recipe> findByStatus(RecipeStatus status) {
    return recipeRepository.findByStatus(status)
            .stream()
            .map(RecipeEntityMapper::toDomain)
            .toList();
  }

  @Override
  public PageResult<Recipe> findByStatus(RecipeStatus status, int size, int page) {
    Objects.requireNonNull(status);
    var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    var pageResult = recipeRepository.findByStatus(status, pageable);
    var recipes = pageResult.getContent().stream()
            .map(RecipeEntityMapper::toDomain)
            .toList();
    return new PageResult<>(recipes, pageResult.getTotalElements());
  }

  @Override
  public PageResult<Recipe> searchRecipes(RecipeSearchCriteria criteria) {
    Objects.requireNonNull(criteria);
    var pageable = PageRequest.of(criteria.page(), criteria.size());
    var pageResult = recipeRepository.searchRecipes(criteria.status(), criteria.search(), criteria.allergens(), pageable);
    var recipes = pageResult.getContent().stream()
            .map(RecipeEntityMapper::toDomain)
            .toList();
    return new PageResult<>(recipes, pageResult.getTotalElements());
  }

  @Override
  public List<Recipe> findByAuthorId(UUID authorId) {
    Objects.requireNonNull(authorId);
    return recipeRepository.findByAuthorId(authorId)
            .stream()
            .map(RecipeEntityMapper::toDomain)
            .toList();
  }

  @Override
  public List<Allergen> findAllAllergens() {
    return allergenRepository.findAll().stream()
            .map(RecipeEntityMapper::toDomain)
            .toList();
  }

  @Override
  public List<String> findAllIngredientNames() {
    return ingredientRepository.findAll().stream()
            .map(e -> e.getName())
            .distinct()
            .sorted()
            .toList();
  }

  @Override
  public Recipe save(Recipe recipe) {
    Objects.requireNonNull(recipe);

    var author = userRepository.findByUsername(recipe.usernameAuthor())
            .orElseThrow(() -> new IllegalStateException("User not found: " + recipe.usernameAuthor()));

    RecipeEntity parent = null;
    if (recipe.parentId() != null) {
      parent = recipeRepository.findById(recipe.parentId())
              .orElseThrow(() -> new IllegalStateException("Parent recipe not found: " + recipe.parentId()));
    }

    var allergenIds = recipe.allergens().stream().map(Allergen::id).toList();
      var allergens = new ArrayList<>(allergenRepository.findAllById(allergenIds));


    var ingredientNames = recipe.ingredients().stream()
            .map(RecipeIngredient::name).toList();
    var ingredients = ingredientRepository.findByNameIn(ingredientNames);


    var entity = RecipeEntityMapper.toEntity(
            recipe, parent, author, List.copyOf(allergens), ingredients
    );
    var saved = recipeRepository.save(entity);

    return RecipeEntityMapper.toDomain(saved);
  }

  @Override
  public void deleteById(UUID id) {
    recipeRepository.deleteById(id);
  }
}
