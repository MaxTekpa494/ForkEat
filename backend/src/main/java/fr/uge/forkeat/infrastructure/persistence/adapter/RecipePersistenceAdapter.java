package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.RecipeEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.IngredientEntity;
import fr.uge.forkeat.infrastructure.persistence.neo4j.repository.Neo4jRecipeRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.*;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import jakarta.persistence.EntityManager;
import fr.uge.forkeat.service.persistence.UserPersistence;
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
  private final Neo4jRecipeRepository neo4jRecipeRepository;
  private final RecipeAllergenRepository recipeAllergenRepository;
  private final RecipeIngredientRepository recipeIngredientRepository;

  private final EntityManager entityManager;

  public RecipePersistenceAdapter(RecipeRepository recipeRepository, UserRepository userRepository,
                                  AllergenRepository allergenRepository, IngredientRepository ingredientRepository,
                                  RecipeAllergenRepository recipeAllergenRepository, RecipeIngredientRepository recipeIngredientRepository,
                                  EntityManager entityManager, Neo4jRecipeRepository neo4jRecipeRepository) {
    this.recipeRepository = recipeRepository;
    this.userRepository = userRepository;
    this.allergenRepository = allergenRepository;
    this.ingredientRepository = ingredientRepository;
    this.recipeAllergenRepository = recipeAllergenRepository;
    this.recipeIngredientRepository = recipeIngredientRepository;
    this.entityManager = entityManager;
    this.neo4jRecipeRepository = neo4jRecipeRepository;
  }

  @Override
  public Optional<Recipe> findById(UUID id) {
    Objects.requireNonNull(id);
    return recipeRepository.findById(id).map(RecipeEntityMapper::toDomain);
  }

  @Override
  public Optional<RecipeWithMetaData> findRecipeWithMetaDataById(UUID recipeId){
      Objects.requireNonNull(recipeId);
      var recipe = recipeRepository.findById(recipeId).map(RecipeEntityMapper::toDomain);
      if(recipe.isEmpty()){
          return Optional.empty();
      }
      var metaData = new RecipeMetaData(nbLike(recipeId));
      return Optional.of(new RecipeWithMetaData(recipe.get(), metaData));
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
  public List<Recipe> findByAuthorUsername(String authorUsername) {
    return recipeRepository.findByAuthorUsername(authorUsername)
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
            .map(IngredientEntity::getName)
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
    var ingredients = getOrCreateIngredients(ingredientNames);

    var entity = RecipeEntityMapper.toEntity(
            recipe, parent, author, List.copyOf(allergens), ingredients
    );
    var saved = recipeRepository.save(entity);

    return RecipeEntityMapper.toDomain(saved);
  }

  @Override
  public Recipe update(UUID id, Recipe recipe) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(recipe);

    // Il faut supprimer les relations entre recipe et (ingredients, allergens)
    // parce que sinon on viole la constrainte de l'unicité sur (id_recipe, id_allergen...)
    // Parce qu'en faisant mettant à jour les ingredients et allergens, on crée une nouvelle relation
    // Alors on peut decider de ne pas recréer mais d'operer juste à des modifation sur
    // RecipeEngredientEntity si l'utilisateur decide de changer la quantité par exemple
    // mais cela me semble plus chère (egalement source d'erreur) que si on supprime tout et ensuite
    // on repersiste. À discuter...
    // D'aillleur le orphanRemoval ne fonctionne qu'à la fin de la transactions (oh joie)
    recipeAllergenRepository.deleteByRecipeId(id);
    recipeIngredientRepository.deleteByRecipeId(id);
    entityManager.flush();
    entityManager.clear();

    var existingEntity = recipeRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("Recipe not found: " + id));

    existingEntity.setTitle(recipe.title());
    existingEntity.setSummary(recipe.summary());
    existingEntity.setPreparationMinutes(recipe.preparationMinutes());
    existingEntity.setImageUrl(recipe.imageUrl());
    existingEntity.setStatus(recipe.status());
    existingEntity.setDietaryFlag(recipe.dietaryFlags());
    existingEntity.setStepByStepInstructions(
            RecipeEntityMapper.toEntitySteps(recipe.stepByStepInstructions())
    );

    var allergenIds = recipe.allergens().stream().map(Allergen::id).toList();
    var allergens = allergenRepository.findAllById(allergenIds);
    existingEntity.setAllergens(
            RecipeEntityMapper.toRecipeAllergenEntities(recipe.allergens(), allergens, existingEntity)
    );

    var ingredientNames = recipe.ingredients().stream().map(RecipeIngredient::name).toList();
    var ingredients = getOrCreateIngredients(ingredientNames);
    existingEntity.setIngredients(
            RecipeEntityMapper.toRecipeIngredientEntities(recipe.ingredients(), ingredients, existingEntity)
    );

    var saved = recipeRepository.save(existingEntity);
    return RecipeEntityMapper.toDomain(saved);
  }


  @Override
  public void deleteById(UUID id) {
    recipeRepository.deleteById(id);
  }


  private long nbLike(UUID recipeId) {
      return neo4jRecipeRepository.nbLike(recipeId);
  }

  /**
   * Récupère les ingrédients existants et crée/persiste les nouveaux si nécessaire
   * Pour eviter le fait que ça plante quand t-on rajoute de nouveaux à la creation/modification d'une
   * Recipe, alors il faut juste que les moderateurs s'assure que les ingredients rajoutés par l'utilisteur sont réels
   */
  private List<IngredientEntity> getOrCreateIngredients(List<String> ingredientNames) {
    var existingIngredients = ingredientRepository.findByNameIn(ingredientNames);

    var existingNames = existingIngredients.stream()
            .map(ing -> ing.getName().toLowerCase())
            .toList();
    var newIngredientNames = ingredientNames.stream()
            .filter(name -> !existingNames.contains(name.toLowerCase()))
            .toList();

    var newIngredients = new ArrayList<IngredientEntity>();
    newIngredientNames.forEach(name -> {
      var newIngredient = new IngredientEntity(
              name,
              "Non catégorisé",
              false
      );
      newIngredients.add(ingredientRepository.save(newIngredient));
    });
    var allIngredients = new ArrayList<>(existingIngredients);
    allIngredients.addAll(newIngredients);
    return List.copyOf(allIngredients);
  }
}
