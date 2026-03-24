package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.RecipeEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.neo4j.node.RecipeNode;
import fr.uge.forkeat.infrastructure.persistence.neo4j.repository.Neo4jRecipeRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.DietaryEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.IngredientEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.SuperLikeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.RecipeSummaryView;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.*;
import fr.uge.forkeat.infrastructure.persistence.sync.cdc.RecipeNodeClient;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipeSummary;
import fr.uge.forkeat.service.model.recipe.projection.RecipeSummary;
import fr.uge.forkeat.service.model.recipe.projection.RecipeRejectionInfo;
import fr.uge.forkeat.service.model.recipe.projection.AuthorRecipeSummary;
import fr.uge.forkeat.service.model.recipe.projection.RecipeCounts;
import fr.uge.forkeat.service.model.recipe.projection.UserRecipeStats;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Component
public final class RecipePersistenceAdapter implements RecipePersistence {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final AllergenRepository allergenRepository;
    private final IngredientRepository ingredientRepository;
    private final Neo4jRecipeRepository neo4jRecipeRepository;
    private final RecipeAllergenRepository recipeAllergenRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeDietaryRepository recipeDietaryRepository;
    private final DietaryRepository dietaryRepository;
    private final EntityManager entityManager;
    private final SuperLikeRepository superLikeRepository;

    public RecipePersistenceAdapter(RecipeRepository recipeRepository, UserRepository userRepository,
                                    AllergenRepository allergenRepository, IngredientRepository ingredientRepository,
                                    Neo4jRecipeRepository neo4jRecipeRepository,
                                    RecipeAllergenRepository recipeAllergenRepository,
                                    RecipeIngredientRepository recipeIngredientRepository,
                                    RecipeDietaryRepository recipeDietaryRepository,
                                    DietaryRepository dietaryRepository, EntityManager entityManager,
                                    SuperLikeRepository superLikeRepository) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.allergenRepository = allergenRepository;
        this.ingredientRepository = ingredientRepository;
        this.neo4jRecipeRepository = neo4jRecipeRepository;
        this.recipeAllergenRepository = recipeAllergenRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.recipeDietaryRepository = recipeDietaryRepository;
        this.entityManager = entityManager;
        this.dietaryRepository = dietaryRepository;
        this.superLikeRepository = superLikeRepository;
    }

    @Override
    public Optional<Recipe> findById(UUID id) {
        Objects.requireNonNull(id);
        return recipeRepository.findById(id).map(RecipeEntityMapper::toDomain);
    }

    @Override
    public boolean existRecipe(UUID id) {
        Objects.requireNonNull(id);
        return recipeRepository.existsById(id);
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
    public PageResult<Recipe> getRecipesToModerate(UUID authorId, int size, int page) {
        Objects.requireNonNull(authorId);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var pageResult = recipeRepository.findByStatusAndAuthorIdNot(RecipeStatus.PENDING_REVIEW, authorId, pageable);
        var recipes = pageResult.getContent().stream()
                .map(RecipeEntityMapper::toDomain)
                .toList();
        return new PageResult<>(recipes, pageResult.getTotalElements());
    }

    @Override
    public PageResult<RecipeSummary> searchRecipes(RecipeSearchCriteria criteria) {
        Objects.requireNonNull(criteria);
        var pageable = PageRequest.of(criteria.page(), criteria.size());
        var pageResult = recipeRepository.searchRecipes(criteria.status(), criteria.search(), criteria.allergens(), pageable);
        var recipes = pageResult.getContent().stream()
                .map(RecipeEntityMapper::toDomain)
                .toList();
        return new PageResult<>(recipes, pageResult.getTotalElements());
    }

    @Override
    public PageResult<RecipeSummary> searchPersonalizedFeedRecipes(String username, Instant beforeTime, int nbPage) {
        var sinceTime = beforeTime.minusSeconds(2678400);
        var pageResult = neo4jRecipeRepository.getFeed(username, nbPage * 20L, nbPage* 20L + 20, sinceTime.atZone(ZoneOffset.UTC), beforeTime.atZone(ZoneOffset.UTC));
        var recipes = recipeRepository.findSummariesByIds(pageResult.stream()
                .map(RecipeNode::getId)
                .toList()).stream().map(r-> new RecipeSummary(r.getId(), r.getTitle(), r.getSummary(), r.getImageUrl(), r.getPreparationMinutes(), r.getCreatedAt(), r.getAuthorUsername())).toList();
        return new PageResult<>(recipes, recipes.size());
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
    public List<String> findAllUnitNames() {
        return recipeIngredientRepository.findAllDistinctUnits();
    }

    @Override
    public List<String> findAllDietaryNames() {
        return dietaryRepository.findAll().stream()
                .map(d -> d.getName())
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

        var dietaries = getOrCreateDietaries(recipe.dietaries());
        var entity = RecipeEntityMapper.toEntity(
                recipe, parent, author, List.copyOf(allergens), ingredients, dietaries
        );
        var saved = recipeRepository.save(entity);

        return RecipeEntityMapper.toDomain(saved);
    }

    @Override
    public Recipe update(UUID id, Recipe recipe) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(recipe);

        // Il faut supprimer les relations entre recipe et (ingredients, allergens)
        // parce que sinon on viole la contrainte de l'unicité sur (id_recipe, id_allergen...)
        // Parce qu'en faisant mettant à jour les ingredients et allergens, on crée une nouvelle relation
        // Alors on peut decider de ne pas recréer mais d'operer juste à des modifation sur
        // RecipeIngredientEntity si l'utilisateur decide de changer la quantité par exemple
        // mais cela me semble plus chère (egalement source d'erreur) que si on supprime tout et ensuite
        // on repersiste. À discuter...
        // D'ailleurs le orphanRemoval ne fonctionne qu'à la fin de la transactions (oh joie)
        recipeAllergenRepository.deleteByRecipeId(id);
        recipeIngredientRepository.deleteByRecipeId(id);
        recipeDietaryRepository.deleteByRecipeId(id);
        entityManager.flush();
        entityManager.clear();

        var existingEntity = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Recipe not found: " + id));

        existingEntity.setTitle(recipe.title());
        existingEntity.setSummary(recipe.summary());
        existingEntity.setPreparationMinutes(recipe.preparationMinutes());
        existingEntity.setImageUrl(recipe.imageUrl());
        existingEntity.setStatus(recipe.status());
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

        var dietaries = getOrCreateDietaries(recipe.dietaries());
        existingEntity.setDietaries(RecipeEntityMapper.toRecipeDietaryEntities(recipe.dietaries(), dietaries, existingEntity));
        var saved = recipeRepository.save(existingEntity);
        return RecipeEntityMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        recipeRepository.deleteById(id);
    }

    @Override
    public boolean isImageUrlUsedByOtherRecipes(UUID excludeRecipeId, String imageUrl) {
        Objects.requireNonNull(excludeRecipeId);
        Objects.requireNonNull(imageUrl);
        return recipeRepository.existsByImageUrlAndIdNot(imageUrl, excludeRecipeId);
    }

    @Override
    public void reparentVariants(UUID deletedId, UUID newParentId) {
        recipeRepository.reparentVariants(deletedId, newParentId);
    }

    @Override
    public void reassignRecipesToUser(UUID fromUserId, UUID toUserId) {
        Objects.requireNonNull(fromUserId);
        Objects.requireNonNull(toUserId);
        recipeRepository.reassignAuthor(fromUserId, toUserId);
    }

    @Override
    public Recipe updateStatus(UUID id, RecipeStatus status) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(status);
    var entity = recipeRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("Recipe not found: " + id));
    entity.setStatus(status);
    return RecipeEntityMapper.toDomain(recipeRepository.save(entity));
    }

    @Override
    public long countByStatus(RecipeStatus status) {
    Objects.requireNonNull(status);
    return recipeRepository.countByStatus(status);
    }

    @Override
    public List<RecipeSummary> findSummariesByIds(List<UUID> ids) {
        Objects.requireNonNull(ids);
        if (ids.isEmpty()) {
            return List.of();
        }
        var viewMap = recipeRepository.findSummariesByIds(ids).stream()
                .collect(Collectors.toMap(
                        RecipeSummaryView::getId,
                        v -> new RecipeSummary(v.getId(), v.getTitle(), v.getSummary(), v.getImageUrl(), v.getPreparationMinutes(), v.getCreatedAt(), v.getAuthorUsername())
                ));
        return ids.stream()
                .map(viewMap::get)
                .filter(Objects::nonNull) // Normalement c'est pas sensé être null car les deux tables sont synchros
                .toList();
    }

    @Override
    public PageResult<RecipeSummary> findUserRecipeSummaries(String username, RecipeStatus status, int size, int page) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(status);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var pageResult = recipeRepository.findByAuthorUsernameAndStatus(username, status, pageable);
        var views = pageResult.getContent();

        if (views.isEmpty()) {
            return new PageResult<>(List.of(), pageResult.getTotalElements());
        }

        var summaries = views.stream()
                .map(s -> new RecipeSummary(
                        s.getId(), s.getTitle(), s.getSummary(), s.getImageUrl(),
                        s.getPreparationMinutes(), s.getCreatedAt(), username
                ))
                .toList();

        return new PageResult<>(summaries, pageResult.getTotalElements());
    }

    @Override
    public RecipeCounts findRecipeCounts(UUID recipeId) {
        Objects.requireNonNull(recipeId);
        return findRecipeCounts(List.of(recipeId)).getOrDefault(recipeId, RecipeCounts.ZERO);
    }

    @Override
    public Map<UUID, RecipeCounts> findRecipeCounts(List<UUID> recipeIds) {
        Objects.requireNonNull(recipeIds);
        if (recipeIds.isEmpty()) {
            return Map.of();
        }
        var recipeIdStrings = recipeIds.stream().map(UUID::toString).toList();
        return neo4jRecipeRepository.findCountsByRecipeIds(recipeIdStrings).stream()
                .collect(Collectors.toMap(
                        r -> UUID.fromString(r.recipeId()),
                        r -> new RecipeCounts(r.likeCount(), r.superLikeCount(), r.followCount())
                ));
    }

    @Override
    public RecipeUserInteraction findUserRecipeInteraction(UUID recipeId, UUID userId) {
        Objects.requireNonNull(recipeId);
        Objects.requireNonNull(userId);
        return findUserRecipeInteractions(List.of(recipeId), userId)
                .getOrDefault(recipeId, RecipeUserInteraction.NONE);
    }

    @Override
    public Map<UUID, RecipeUserInteraction> findUserRecipeInteractions(List<UUID> recipeIds, UUID userId) {
        Objects.requireNonNull(recipeIds);
        Objects.requireNonNull(userId);
        if (recipeIds.isEmpty()) {
            return Map.of();
        }
        var recipeIdStrings = recipeIds.stream().map(UUID::toString).toList();
        return neo4jRecipeRepository.findUserInteractionsByRecipeIds(recipeIdStrings, userId.toString()).stream()
                .collect(Collectors.toMap(
                        r -> UUID.fromString(r.recipeId()),
                        r -> new RecipeUserInteraction(r.likedByCurrentUser(), r.superLikedByCurrentUser(), r.followedByCurrentUser())
                ));
    }

    @Override
    public Optional<PersonalizedRecipeSummary> findTopLikedPublishedRecipe(UUID userId) {
        var topIds = neo4jRecipeRepository.findTopLikedRecipeIds(10);
        for (var idStr : topIds) {
            var uuid = UUID.fromString(idStr);
            var opt = recipeRepository.findById(uuid);
            if (opt.isPresent() && opt.get().getStatus() == RecipeStatus.PUBLISHED) {
                var entity = opt.get();
                var summary = new RecipeSummary(
                        entity.getId(), entity.getTitle(), entity.getSummary(),
                        entity.getImageUrl(), entity.getPreparationMinutes(),
                        entity.getCreatedAt(), entity.getAuthor().getUsername()
                );
                var counts = findRecipeCounts(uuid);
                var interaction = userId != null
                        ? findUserRecipeInteraction(uuid, userId)
                        : RecipeUserInteraction.NONE;
                return Optional.of(new PersonalizedRecipeSummary(summary, counts, interaction));
            }
        }
        return Optional.empty();
    }

    @Override
    public long countByAuthorUsername(String username) {
        Objects.requireNonNull(username);
        return neo4jRecipeRepository.countByAuthorUsername(username);
    }

    @Override
    public void likeRecipe(UUID userId, UUID recipeId) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(recipeId);
        neo4jRecipeRepository.likeRecipe(userId, recipeId);
    }

    @Override
    public void unlikeRecipe(UUID userId, UUID recipeId) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(recipeId);
        neo4jRecipeRepository.unlikeRecipe(userId, recipeId);
    }

    @Override
    public void followRecipe(UUID userId, UUID recipeId) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(recipeId);
        neo4jRecipeRepository.followRecipe(userId, recipeId, Instant.now());
    }

    @Override
    public void unfollowRecipe(UUID userId, UUID recipeId) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(recipeId);
        neo4jRecipeRepository.unfollowRecipe(userId, recipeId);
    }

    @Override
    public void superLikeRecipe(UUID userId, UUID recipeId, long amount, UUID promotionId, boolean isBonusFree, long redistAmountCents){
        var superLike = new SuperLikeEntity(userId, recipeId, amount, promotionId, isBonusFree, redistAmountCents);
        superLikeRepository.save(superLike);
    }

    @Override
    public boolean hasSuperLikedRecipe(UUID userId, UUID recipeId){
        return superLikeRepository.existsByRecipeIdAndUserId(Objects.requireNonNull(userId), Objects.requireNonNull(recipeId));
    }

    @Override
    public PageResult<AuthorRecipeSummary> findRecipesByAuthor(UUID authorId, RecipeStatus status, int page, int size) {
        Objects.requireNonNull(authorId);
        Objects.requireNonNull(status);
        var pageable = PageRequest.of(page, size);
        var pageResult = recipeRepository.findRecipeSummariesByAuthorIdAndStatus(authorId, status.name(), pageable);
        var summaries = pageResult.getContent().stream()
                .map(v -> {
                    var summary = new RecipeSummary(v.getId(), v.getTitle(), v.getSummary(), v.getImageUrl(), v.getPreparationMinutes(), v.getCreatedAt(), v.getAuthorUsername());
                    var rejectionInfo = v.getJustification() != null
                            ? new RecipeRejectionInfo(v.getJustification(), v.getRejectedAt())
                            : null;
                    return new AuthorRecipeSummary(summary, RecipeStatus.valueOf(v.getStatus()), rejectionInfo);
                })
                .toList();
        return new PageResult<>(summaries, pageResult.getTotalElements());
    }

    @Override
    public UserRecipeStats countRecipesByAuthorGroupedByStatus(UUID authorId) {
        Objects.requireNonNull(authorId);
        var counts = recipeRepository.countByAuthorIdGroupByStatus(authorId);
        long published = 0, draft = 0, pendingReview = 0, rejected = 0;
        for (var row : counts) {
            switch (RecipeStatus.valueOf(row.getStatus())) {
                case PUBLISHED -> published = row.getCount();
                case DRAFT -> draft = row.getCount();
                case PENDING_REVIEW -> pendingReview = row.getCount();
                case REJECTED -> rejected = row.getCount();
            }
        }
        return new UserRecipeStats(published, draft, pendingReview, rejected);
    }

    @Override
    public boolean isAuthor(UUID recipeId, UUID authorId) {
        Objects.requireNonNull(recipeId);
        Objects.requireNonNull(authorId);
        return recipeRepository.existsByIdAndAuthorId(recipeId, authorId);
    }

    /**
     * Récupère les ingrédients existants et crée/persiste les nouveaux si nécessaire.
     * Pour eviter le fait que ça plante quand on rajoute de nouveaux à la creation/modification d'une
     * Recipe, alors il faut juste que les moderateurs s'assure que les ingredients rajoutés par l'utilisateur sont réels.
     */
    private List<IngredientEntity> getOrCreateIngredients(List<String> ingredientNames) {
        var existingIngredients = ingredientRepository.findByNameIn(ingredientNames);

        var existingNames = existingIngredients.stream()
                .map(ing -> ing.getName().toLowerCase())
                .toList();
        // Ici en plus de ça, on peut faire une distance de levenshtein
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

    private List<DietaryEntity> getOrCreateDietaries(List<String> dietariesNames){
        var existingDietaries = dietaryRepository.findByNameIn(dietariesNames);
        var existingNames = existingDietaries.stream().map(die ->
                die.getName().toLowerCase()).toList();
        var newDietariesNames = dietariesNames.stream().filter(name -> !existingNames.contains(name.toLowerCase())).toList();
        var newDietaries = new ArrayList<DietaryEntity>();
        newDietariesNames.forEach(name -> {
            var newDietary = new DietaryEntity(name);
            newDietaries.add(dietaryRepository.save(newDietary));
        });
        var allDietaries = new ArrayList<>(existingDietaries);
        allDietaries.addAll(newDietaries);
        return List.copyOf(allDietaries);
    }
}
