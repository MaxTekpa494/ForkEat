package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.RecipeEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.AllergenEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.IngredientEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.AllergenRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.IngredientRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Allergen;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeIngredient;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import org.springframework.data.domain.PageRequest;
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
    this.recipeRepository = Objects.requireNonNull(recipeRepository); // Est-ce qu'on doit mettre les required non null
    this.userRepository = Objects.requireNonNull(userRepository);
    this.allergenRepository = Objects.requireNonNull(allergenRepository);
    this.ingredientRepository = Objects.requireNonNull(ingredientRepository);
  }


  @Override
  public Optional<Recipe> findById(UUID id) {
    Objects.requireNonNull(id); // ??? Voir si on fait les requireNonNull
    return recipeRepository.findById(id).map(RecipeEntityMapper::toDomain);
  }

  @Override
  public List<Recipe> findByStatus(String status) {
    // Objects.requireNonNull(status) ???
    return recipeRepository.findByStatus(RecipeStatus.valueOf(status)) // Ici faudra voir comment postgresql fait la conversion
            .stream()
            .map(RecipeEntityMapper::toDomain)
            .toList();
  }

  @Override
  public PageResult<Recipe> findByStatus(String status, int size, int page) {
    Objects.requireNonNull(status);
    var pageable = PageRequest.of(page, size);
    var pageResult = recipeRepository.findByStatus(RecipeStatus.valueOf(status), pageable);
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
    var allergens = new ArrayList<AllergenEntity>();
    allergenRepository.findAllById(allergenIds)
            .forEach(allergens::add);


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
