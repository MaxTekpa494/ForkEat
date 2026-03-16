package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.RecipeUserInteractionProjection;
import fr.uge.forkeat.infrastructure.persistence.neo4j.repository.Neo4jRecipeRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.*;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.RecipeBaseSummaryView;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.RecipeSummaryView;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.*;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipePersistenceAdapterTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private Neo4jRecipeRepository neo4jRecipeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AllergenRepository allergenRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private RecipeIngredientRepository recipeIngredientRepository;
    @Mock
    private RecipeAllergenRepository recipeAllergenRepository;
    @Mock
    private RecipeDietaryRepository recipeDietaryRepository;

    @Mock
    private DietaryRepository dietaryRepository;


    @Mock
    private SuperLikeRepository superLikeRepository;

    @Mock
    private EntityManager entityManager;

    private RecipePersistenceAdapter adapter;
    private UserEntity author;
    private Instant now;


    @BeforeEach
    void setUp() {
        adapter = new RecipePersistenceAdapter(
                recipeRepository, userRepository,
                allergenRepository, ingredientRepository,
                neo4jRecipeRepository,
                recipeAllergenRepository, recipeIngredientRepository,
                recipeDietaryRepository, dietaryRepository, entityManager, superLikeRepository);
        now = Instant.now();

        author = new UserEntity();
        author.setId(UUID.randomUUID());
        author.setUsername("chef_test");
        author.setFirstName("Test");
        author.setLastName("Chef");
        author.setEmail("test@example.com");
        author.setPassword("hashedpassword");
        author.setRole(UserRole.MEMBER);
        author.setStatus(UserStatus.ACTIVE);
        author.setAuthMode(AuthMode.LOCAL);
    }

    @Nested
    class FindById {

        @Test
        void shouldReturnRecipeWhenFound() {
            var recipeId = UUID.randomUUID();
            var entity = createRecipeEntity(recipeId, "Tarte aux pommes", RecipeStatus.PUBLISHED);
            when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(entity));

            var result = adapter.findById(recipeId);

            assertTrue(result.isPresent());
            assertEquals(recipeId, result.get().id());
            assertEquals("Tarte aux pommes", result.get().title());
            verify(recipeRepository).findById(recipeId);
        }

        @Test
        void shouldReturnEmptyWhenNotFound() {
            var recipeId = UUID.randomUUID();
            when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

            var result = adapter.findById(recipeId);

            assertTrue(result.isEmpty());
            verify(recipeRepository).findById(recipeId);
        }

        @Test
        void shouldThrowWhenIdIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.findById(null));
        }
    }

    @Nested
    class FindByStatus {

        @Test
        void shouldReturnMatchingRecipes() {
            var entity1 = createRecipeEntity(UUID.randomUUID(), "Recette 1", RecipeStatus.PUBLISHED);
            var entity2 = createRecipeEntity(UUID.randomUUID(), "Recette 2", RecipeStatus.PUBLISHED);
            when(recipeRepository.findByStatus(RecipeStatus.PUBLISHED)).thenReturn(List.of(entity1, entity2));

            var result = adapter.findByStatus(RecipeStatus.PUBLISHED);

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(r -> r.status() == RecipeStatus.PUBLISHED));
            verify(recipeRepository).findByStatus(RecipeStatus.PUBLISHED);
        }

        @Test
        void shouldReturnEmptyListWhenNoMatch() {
            when(recipeRepository.findByStatus(RecipeStatus.DRAFT)).thenReturn(List.of());

            var result = adapter.findByStatus(RecipeStatus.DRAFT);

            assertTrue(result.isEmpty());
            verify(recipeRepository).findByStatus(RecipeStatus.DRAFT);
        }
    }

    @Nested
    class GetRecipesToModerate {
        private final Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        private final PageRequest pageRequest = PageRequest.of(0, 10, sort);
        @Test
        void shouldReturnPendingRecipesExcludingModeratorOnes() {
            var moderator = "moderator1";
            var entity1 = createRecipeEntity(UUID.randomUUID(), "Recette 1", RecipeStatus.PENDING_REVIEW);
            var entity2 = createRecipeEntity(UUID.randomUUID(), "Recette 2", RecipeStatus.PENDING_REVIEW);
            var entity3 = createRecipeEntity(UUID.randomUUID(), "Recette 3", RecipeStatus.PENDING_REVIEW);
            entity1.setAuthor(author);
            var otherAuthor = new UserEntity();
            otherAuthor.setId(UUID.randomUUID());
            otherAuthor.setUsername("user2");
            entity2.setAuthor(otherAuthor);
            var moderatorAuthor = new UserEntity();
            moderatorAuthor.setId(UUID.randomUUID());
            moderatorAuthor.setUsername(moderator);
            entity3.setAuthor(moderatorAuthor);
            var page = new PageImpl<>(List.of(entity1, entity2), pageRequest, 2);
            when(recipeRepository.findByStatusAndAuthorUsernameNot(RecipeStatus.PENDING_REVIEW, moderator, pageRequest))
                    .thenReturn(page);

            var result = adapter.getRecipesToModerate(moderator, 10, 0);

            assertEquals(2, result.items().size());
            assertTrue(result.items().stream().noneMatch(r -> r.usernameAuthor().equals(moderator)));
            assertEquals(2, result.total());
            verify(recipeRepository).findByStatusAndAuthorUsernameNot(RecipeStatus.PENDING_REVIEW, moderator, pageRequest);
        }

        @Test
        void shouldReturnEmptyIfAllPendingAreFromModerator() {
            var moderator = "moderator1";
            var moderatorAuthor = new UserEntity();
            moderatorAuthor.setId(UUID.randomUUID());
            moderatorAuthor.setUsername(moderator);
            var entity = createRecipeEntity(UUID.randomUUID(), "Recette du modérateur", RecipeStatus.PENDING_REVIEW);
            entity.setAuthor(moderatorAuthor);
            Page<RecipeEntity> page = new PageImpl<>(java.util.Collections.emptyList(), pageRequest, 0);
            when(recipeRepository.findByStatusAndAuthorUsernameNot(RecipeStatus.PENDING_REVIEW, moderator, pageRequest))
                .thenReturn(page);

            var result = adapter.getRecipesToModerate(moderator, 10, 0);

            assertTrue(result.items().isEmpty());
            assertEquals(0, result.total());
        }

        @Test
        void shouldReturnAllIfNoPendingFromModerator() {
            var moderator = "moderator1";
            var entity1 = createRecipeEntity(UUID.randomUUID(), "Recette 1", RecipeStatus.PENDING_REVIEW);
            var entity2 = createRecipeEntity(UUID.randomUUID(), "Recette 2", RecipeStatus.PENDING_REVIEW);
            entity1.setAuthor(author);
            var otherAuthor = new UserEntity();
            otherAuthor.setId(UUID.randomUUID());
            otherAuthor.setUsername("user2");
            entity2.setAuthor(otherAuthor);
            var page = new PageImpl<>(List.of(entity1, entity2), pageRequest, 2);
            when(recipeRepository.findByStatusAndAuthorUsernameNot(RecipeStatus.PENDING_REVIEW, moderator, pageRequest))
                    .thenReturn(page);

            var result = adapter.getRecipesToModerate(moderator, 10, 0);

            assertEquals(2, result.items().size());
            assertTrue(result.items().stream().noneMatch(r -> r.usernameAuthor().equals(moderator)));
        }

        @Test
        void shouldThrowWhenModeratorIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.getRecipesToModerate(null, 10, 0));
        }
    }

    @Nested
    class FindByAuthorId {

        @Test
        void shouldReturnAuthorRecipes() {
            var authorId = author.getId();
            var entity = createRecipeEntity(UUID.randomUUID(), "Ma recette", RecipeStatus.DRAFT);
            when(recipeRepository.findByAuthorId(authorId)).thenReturn(List.of(entity));

            var result = adapter.findByAuthorId(authorId);

            assertEquals(1, result.size());
            assertEquals("Ma recette", result.getFirst().title());
            verify(recipeRepository).findByAuthorId(authorId);
        }

        @Test
        void shouldThrowWhenAuthorIdIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.findByAuthorId(null));
        }

        @Test
        void shouldReturnEmptyListWhenAuthorHasNoRecipes() {
            var authorId = UUID.randomUUID();
            when(recipeRepository.findByAuthorId(authorId)).thenReturn(List.of());

            var result = adapter.findByAuthorId(authorId);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class Save {

        @Test
        void shouldPersistRecipeAndReturnIt() {
            var recipeId = UUID.randomUUID();
            var recipe = createRecipe(recipeId, "Nouvelle recette", RecipeStatus.DRAFT);
            var savedEntity = createRecipeEntity(recipeId, "Nouvelle recette", RecipeStatus.DRAFT);

            when(userRepository.findByUsername("chef_test")).thenReturn(Optional.of(author));
            when(allergenRepository.findAllById(any())).thenReturn(List.of());
            when(ingredientRepository.findByNameIn(any())).thenReturn(List.of());
            when(recipeRepository.save(any(RecipeEntity.class))).thenReturn(savedEntity);

            var result = adapter.save(recipe);

            assertNotNull(result);
            assertEquals(recipeId, result.id());
            assertEquals("Nouvelle recette", result.title());
            verify(recipeRepository).save(any(RecipeEntity.class));
        }

        @Test
        void shouldThrowWhenRecipeIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.save(null));
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            var recipe = createRecipe(UUID.randomUUID(), "Test", RecipeStatus.DRAFT);
            when(userRepository.findByUsername("chef_test")).thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class, () -> adapter.save(recipe));
        }

        @Test
        void shouldHandleRecipeWithParent() {
            var parentId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            var parentEntity = createRecipeEntity(parentId, "Parent", RecipeStatus.PUBLISHED);
            var recipe = new Recipe(
                    recipeId, "Variante", "Summary", parentId, "chef_test",
                    30, null, RecipeStatus.DRAFT, List.of(), List.of(), List.of(), List.of(), now, now
            );
            var savedEntity = createRecipeEntity(recipeId, "Variante", RecipeStatus.DRAFT);

            when(userRepository.findByUsername("chef_test")).thenReturn(Optional.of(author));
            when(recipeRepository.findById(parentId)).thenReturn(Optional.of(parentEntity));
            when(allergenRepository.findAllById(any())).thenReturn(List.of());
            when(ingredientRepository.findByNameIn(any())).thenReturn(List.of());
            when(recipeRepository.save(any(RecipeEntity.class))).thenReturn(savedEntity);

            var result = adapter.save(recipe);

            assertNotNull(result);
            verify(recipeRepository).findById(parentId);
        }

        @Test
        void shouldThrowWhenParentNotFound() {
            var parentId = UUID.randomUUID();
            var recipe = new Recipe(
                    UUID.randomUUID(), "Variante", "Summary", parentId, "chef_test",
                    30, null, RecipeStatus.DRAFT, List.of(), List.of(), List.of(), List.of(), now, now
            );

            when(userRepository.findByUsername("chef_test")).thenReturn(Optional.of(author));
            when(recipeRepository.findById(parentId)).thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class, () -> adapter.save(recipe));
        }

        @Test
        void shouldHandleRecipeWithAllergens() {
            var recipeId = UUID.randomUUID();
            var allergenId = UUID.randomUUID();
            var allergenEntity = new AllergenEntity("Gluten", AllergenSeverity.HIGH);
            allergenEntity.setId(allergenId);

            var recipe = new Recipe(
                    recipeId, "Recette avec allergene", "Summary", null, "chef_test",
                    30, null, RecipeStatus.DRAFT, List.of(), List.of(),
                    List.of(new Allergen(allergenId, "Gluten", AllergenSeverity.HIGH)), List.of(), now, now
            );
            var savedEntity = createRecipeEntity(recipeId, "Recette avec allergene", RecipeStatus.DRAFT);

            when(userRepository.findByUsername("chef_test")).thenReturn(Optional.of(author));
            when(allergenRepository.findAllById(List.of(allergenId))).thenReturn(List.of(allergenEntity));
            when(ingredientRepository.findByNameIn(any())).thenReturn(List.of());
            when(recipeRepository.save(any(RecipeEntity.class))).thenReturn(savedEntity);

            var result = adapter.save(recipe);

            assertNotNull(result);
            verify(allergenRepository).findAllById(List.of(allergenId));
        }

        @Test
        void shouldHandleRecipeWithIngredients() {
            var recipeId = UUID.randomUUID();
            var ingredientEntity = new IngredientEntity("Farine", "Cereale", false);
            ingredientEntity.setId(UUID.randomUUID());

            var recipe = new Recipe(
                    recipeId, "Recette avec ingredient", "Summary", null, "chef_test",
                    30, null, RecipeStatus.DRAFT, List.of(),
                    List.of(new RecipeIngredient("Farine", 250.0, "g")),
                    List.of(), List.of(), now, now
            );
            var savedEntity = createRecipeEntity(recipeId, "Recette avec ingredient", RecipeStatus.DRAFT);

            when(userRepository.findByUsername("chef_test")).thenReturn(Optional.of(author));
            when(allergenRepository.findAllById(any())).thenReturn(List.of());
            when(ingredientRepository.findByNameIn(List.of("Farine"))).thenReturn(List.of(ingredientEntity));
            when(recipeRepository.save(any(RecipeEntity.class))).thenReturn(savedEntity);

            var result = adapter.save(recipe);

            assertNotNull(result);
            verify(ingredientRepository).findByNameIn(List.of("Farine"));
        }
    }

    @Nested
    class SearchRecipes {

        private RecipeSummaryView createSummaryView(UUID id, String title, String authorUsername) {
            var view = mock(RecipeSummaryView.class);
            when(view.getId()).thenReturn(id);
            when(view.getTitle()).thenReturn(title);
            when(view.getSummary()).thenReturn("Summary for " + title);
            when(view.getImageUrl()).thenReturn(null);
            when(view.getPreparationMinutes()).thenReturn(30);
            when(view.getCreatedAt()).thenReturn(now);
            when(view.getAuthorUsername()).thenReturn(authorUsername);
            return view;
        }

        @Test
        void shouldDelegateToRepository() {
            var id = UUID.randomUUID();
            var view = createSummaryView(id, "Tarte aux pommes", "chef_test");
            var page = new PageImpl<>(List.of(view), PageRequest.of(0, 12), 1);
            when(recipeRepository.searchRecipes(RecipeStatus.PUBLISHED, "tarte", List.of(), PageRequest.of(0, 12)))
                    .thenReturn(page);

            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, "tarte", List.of(), 12, 0);
            var result = adapter.searchRecipes(criteria);

            assertEquals(1, result.items().size());
            assertEquals("Tarte aux pommes", result.items().getFirst().title());
            assertEquals("chef_test", result.items().getFirst().authorUsername());
            assertEquals(1L, result.total());
            verify(recipeRepository).searchRecipes(RecipeStatus.PUBLISHED, "tarte", List.of(), PageRequest.of(0, 12));
        }

        @Test
        void shouldMapViewsToDomain() {
            var view1 = createSummaryView(UUID.randomUUID(), "Recette 1", "user1");
            var view2 = createSummaryView(UUID.randomUUID(), "Recette 2", "user2");
            var page = new PageImpl<>(List.of(view1, view2), PageRequest.of(0, 12), 2);
            when(recipeRepository.searchRecipes(RecipeStatus.PUBLISHED, "recette", List.of(), PageRequest.of(0, 12)))
                    .thenReturn(page);

            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, "recette", List.of(), 12, 0);
            var result = adapter.searchRecipes(criteria);

            assertEquals(2, result.items().size());
            assertEquals(List.of("Recette 1", "Recette 2"),
                    result.items().stream().map(r -> r.title()).toList());
        }

        @Test
        void shouldWorkWithAllergens() {
            var view = createSummaryView(UUID.randomUUID(), "Salade", "chef_test");
            var allergens = List.of("Gluten");
            var page = new PageImpl<>(List.of(view), PageRequest.of(0, 12), 1);
            when(recipeRepository.searchRecipes(RecipeStatus.PUBLISHED, "salade", allergens, PageRequest.of(0, 12)))
                    .thenReturn(page);

            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, "salade", allergens, 12, 0);
            var result = adapter.searchRecipes(criteria);

            assertEquals(1, result.items().size());
            assertEquals(1L, result.total());
            verify(recipeRepository).searchRecipes(RecipeStatus.PUBLISHED, "salade", allergens, PageRequest.of(0, 12));
        }

        @Test
        void shouldThrowWhenCriteriaIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.searchRecipes(null));
        }
    }

    @Nested
    class FindAllAllergens {

        @Test
        void shouldReturnMappedAllergens() {
            var allergen1 = new AllergenEntity("Gluten", AllergenSeverity.HIGH);
            allergen1.setId(UUID.randomUUID());
            var allergen2 = new AllergenEntity("Lactose", AllergenSeverity.MEDIUM);
            allergen2.setId(UUID.randomUUID());
            when(allergenRepository.findAll()).thenReturn(List.of(allergen1, allergen2));

            var result = adapter.findAllAllergens();

            assertEquals(2, result.size());
            assertEquals("Gluten", result.get(0).name());
            assertEquals(AllergenSeverity.HIGH, result.get(0).severity());
            assertEquals("Lactose", result.get(1).name());
            verify(allergenRepository).findAll();
        }

        @Test
        void shouldReturnEmptyListWhenNoAllergens() {
            when(allergenRepository.findAll()).thenReturn(List.of());

            var result = adapter.findAllAllergens();

            assertTrue(result.isEmpty());
            verify(allergenRepository).findAll();
        }
    }

    @Nested
    class ExistRecipe {

        @Test
        void shouldReturnTrueWhenRecipeExists() {
            var id = UUID.randomUUID();
            when(recipeRepository.existsById(id)).thenReturn(true);

            assertTrue(adapter.existRecipe(id));
            verify(recipeRepository).existsById(id);
        }

        @Test
        void shouldReturnFalseWhenRecipeDoesNotExist() {
            var id = UUID.randomUUID();
            when(recipeRepository.existsById(id)).thenReturn(false);

            assertFalse(adapter.existRecipe(id));
            verify(recipeRepository).existsById(id);
        }

        @Test
        void shouldThrowWhenIdIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.existRecipe(null));
        }
    }

    @Nested
    class DeleteById {

        @Test
        void shouldCallRepositoryDelete() {
            var recipeId = UUID.randomUUID();

            adapter.deleteById(recipeId);

            verify(recipeRepository).deleteById(recipeId);
        }
    }

    @Nested
    class FindAllDietaryNames {

        @Test
        void shouldReturnSortedDietaryNames() {
            var vegan = new DietaryEntity("vegan");
            vegan.setId(UUID.randomUUID());
            var halal = new DietaryEntity("halal");
            halal.setId(UUID.randomUUID());
            var vegetarian = new DietaryEntity("végétarien");
            vegetarian.setId(UUID.randomUUID());
            when(dietaryRepository.findAll()).thenReturn(List.of(vegan, halal, vegetarian));

            var result = adapter.findAllDietaryNames();

            assertEquals(3, result.size());
            assertEquals(List.of("halal", "vegan", "végétarien"), result);
            verify(dietaryRepository).findAll();
        }

        @Test
        void shouldReturnEmptyListWhenNoDietaries() {
            when(dietaryRepository.findAll()).thenReturn(List.of());

            var result = adapter.findAllDietaryNames();

            assertTrue(result.isEmpty());
            verify(dietaryRepository).findAll();
        }
    }

    @Nested
    class FindRecipeSummaries {

        @Test
        void shouldReturnSummariesWithCounts() {
            var recipeId = UUID.randomUUID();
            var summaryView = mock(RecipeBaseSummaryView.class);
            when(summaryView.getId()).thenReturn(recipeId);
            when(summaryView.getTitle()).thenReturn("Tarte aux pommes");
            when(summaryView.getSummary()).thenReturn("Délicieuse tarte");
            when(summaryView.getImageUrl()).thenReturn("https://image.com/tarte.jpg");
            when(summaryView.getPreparationMinutes()).thenReturn(45);
            when(summaryView.getCreatedAt()).thenReturn(now);

            var page = new PageImpl<>(List.of(summaryView), PageRequest.of(0, 10), 1);
            when(recipeRepository.findByAuthorUsernameAndStatus(eq("chef_test"), eq(RecipeStatus.PUBLISHED), any()))
                    .thenReturn(page);

            var result = adapter.findUserRecipeSummaries("chef_test", RecipeStatus.PUBLISHED, 10, 0);

            assertEquals(1, result.items().size());
            assertEquals(1L, result.total());
            var item = result.items().getFirst();
            assertEquals("Tarte aux pommes", item.title());
        }

        @Test
        void shouldReturnEmpty_WhenNoRecipesMatchStatus() {
            Page<RecipeBaseSummaryView> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(recipeRepository.findByAuthorUsernameAndStatus(eq("chef_test"), eq(RecipeStatus.PUBLISHED), any()))
                    .thenReturn(page);

            var result = adapter.findUserRecipeSummaries("chef_test", RecipeStatus.PUBLISHED, 10, 0);

            assertTrue(result.items().isEmpty());
            assertEquals(0L, result.total());
            verifyNoInteractions(neo4jRecipeRepository);
        }

        @Test
        void shouldReturnZeroCounts_WhenRecipeNotInNeo4j() {
            var recipeId = UUID.randomUUID();
            var summaryView = mock(RecipeBaseSummaryView.class);
            when(summaryView.getId()).thenReturn(recipeId);
            when(summaryView.getTitle()).thenReturn("Quiche");
            when(summaryView.getSummary()).thenReturn("Bonne quiche");
            when(summaryView.getImageUrl()).thenReturn(null);
            when(summaryView.getPreparationMinutes()).thenReturn(30);
            when(summaryView.getCreatedAt()).thenReturn(now);

            var page = new PageImpl<>(List.of(summaryView), PageRequest.of(0, 10), 1);
            when(recipeRepository.findByAuthorUsernameAndStatus(eq("chef_test"), eq(RecipeStatus.PUBLISHED), any()))
                    .thenReturn(page);

            var result = adapter.findUserRecipeSummaries("chef_test", RecipeStatus.PUBLISHED, 10, 0);

            var item = result.items().getFirst();
            assertNotNull(item);
        }

        @Test
        void shouldThrow_WhenUsernameIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.findUserRecipeSummaries(null, RecipeStatus.PUBLISHED, 10, 0));
        }

        @Test
        void shouldThrow_WhenStatusIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.findUserRecipeSummaries("chef_test", null, 10, 0));
        }
    }

    @Nested
    class FindUserRecipeInteractions {

        @Test
        void shouldReturnInteractionsForUser() {
            var recipeId = UUID.randomUUID();
            var projection = new RecipeUserInteractionProjection(recipeId.toString(), true, false, true);
            when(neo4jRecipeRepository.findUserInteractionsByRecipeIds(List.of(recipeId.toString()), "viewer"))
                    .thenReturn(List.of(projection));

            var result = adapter.findUserRecipeInteractions(List.of(recipeId), "viewer");

            assertEquals(1, result.size());
            var interaction = result.get(recipeId);
            assertNotNull(interaction);
            assertTrue(interaction.likedByCurrentUser());
            assertFalse(interaction.superLikedByCurrentUser());
            assertTrue(interaction.followedByCurrentUser());
        }

        @Test
        void shouldReturnEmptyMap_WhenRecipeIdsIsEmpty() {
            var result = adapter.findUserRecipeInteractions(List.of(), "viewer");

            assertTrue(result.isEmpty());
            verifyNoInteractions(neo4jRecipeRepository);
        }

        @Test
        void shouldReturnEmptyMap_WhenNoInteractionsFound() {
            var recipeId = UUID.randomUUID();
            when(neo4jRecipeRepository.findUserInteractionsByRecipeIds(any(), any())).thenReturn(List.of());

            var result = adapter.findUserRecipeInteractions(List.of(recipeId), "viewer");

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldThrow_WhenRecipeIdsIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.findUserRecipeInteractions(null, "viewer"));
        }

        @Test
        void shouldThrow_WhenCurrentUsernameIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.findUserRecipeInteractions(List.of(UUID.randomUUID()), null));
        }
    }

    @Nested
    class Update {

        @Test
        void shouldUpdateRecipeSuccessfully() {
            var recipeId = UUID.randomUUID();
            var existingEntity = createRecipeEntity(recipeId, "Old Title", RecipeStatus.DRAFT);
            var updatedRecipe = new Recipe(
                    recipeId, "Updated Title", "Updated summary", null, "chef_test",
                    45, "http://new-image.jpg", RecipeStatus.PUBLISHED,
                    List.of(new fr.uge.forkeat.service.model.recipe.RecipeStep(1, "New step")),
                    List.of(), List.of(), List.of(), now, now
            );

            when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(existingEntity));
            when(allergenRepository.findAllById(any())).thenReturn(List.of());
            when(ingredientRepository.findByNameIn(any())).thenReturn(List.of());
            when(dietaryRepository.findByNameIn(any())).thenReturn(List.of());
            when(recipeRepository.save(any(RecipeEntity.class))).thenReturn(existingEntity);

            var result = adapter.update(recipeId, updatedRecipe);

            assertNotNull(result);
            assertEquals("Updated Title", result.title());
            assertEquals("Updated summary", result.summary());
            assertEquals(45, result.preparationMinutes());
            assertEquals(RecipeStatus.PUBLISHED, result.status());
            verify(recipeAllergenRepository).deleteByRecipeId(recipeId);
            verify(recipeIngredientRepository).deleteByRecipeId(recipeId);
            verify(recipeDietaryRepository).deleteByRecipeId(recipeId);
            verify(entityManager).flush();
            verify(entityManager).clear();
            verify(recipeRepository).save(any(RecipeEntity.class));
        }

        @Test
        void shouldThrowWhenRecipeNotFound() {
            var recipeId = UUID.randomUUID();
            var recipe = createRecipe(recipeId, "Test", RecipeStatus.DRAFT);

            when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class, () -> adapter.update(recipeId, recipe));
            verify(recipeAllergenRepository).deleteByRecipeId(recipeId);
            verify(recipeIngredientRepository).deleteByRecipeId(recipeId);
        }

        @Test
        void shouldThrowWhenIdIsNull() {
            var recipe = createRecipe(UUID.randomUUID(), "Test", RecipeStatus.DRAFT);

            assertThrows(NullPointerException.class, () -> adapter.update(null, recipe));
        }

        @Test
        void shouldThrowWhenRecipeIsNull() {
            var recipeId = UUID.randomUUID();

            assertThrows(NullPointerException.class, () -> adapter.update(recipeId, null));
        }

        @Test
        void shouldUpdateRecipeWithNewIngredients() {
            var recipeId = UUID.randomUUID();
            var existingEntity = createRecipeEntity(recipeId, "Recipe", RecipeStatus.DRAFT);

            var existingIngredient = new IngredientEntity("Farine", "Cereale", false);
            existingIngredient.setId(UUID.randomUUID());

            var updatedRecipe = new Recipe(
                    recipeId, "Recipe", "Summary", null, "chef_test",
                    30, null, RecipeStatus.DRAFT,
                    List.of(),
                    List.of(
                            new RecipeIngredient("Farine", 250.0, "g"),
                            new RecipeIngredient("Nouvel Ingredient", 100.0, "g")
                    ),
                    List.of(), List.of(), now, now
            );

            when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(existingEntity));
            when(ingredientRepository.findByNameIn(List.of("Farine", "Nouvel Ingredient")))
                    .thenReturn(List.of(existingIngredient));
            when(allergenRepository.findAllById(any())).thenReturn(List.of());
            when(recipeRepository.save(any(RecipeEntity.class))).thenReturn(existingEntity);

            var newIngredient = new IngredientEntity("Nouvel Ingredient", "Non catégorisé", false);
            newIngredient.setId(UUID.randomUUID());
            when(ingredientRepository.save(any(IngredientEntity.class))).thenReturn(newIngredient);

            var result = adapter.update(recipeId, updatedRecipe);

            assertNotNull(result);
            verify(recipeIngredientRepository).deleteByRecipeId(recipeId);
            verify(entityManager).flush();
            verify(entityManager).clear();
        }

        @Test
        void shouldUpdateRecipeWithAllergens() {
            var recipeId = UUID.randomUUID();
            var allergenId = UUID.randomUUID();
            var existingEntity = createRecipeEntity(recipeId, "Recipe", RecipeStatus.DRAFT);
            var allergenEntity = new AllergenEntity("Gluten", AllergenSeverity.HIGH);
            allergenEntity.setId(allergenId);

            var updatedRecipe = new Recipe(
                    recipeId, "Recipe", "Summary", null, "chef_test",
                    30, null, RecipeStatus.DRAFT,
                    List.of(), List.of(),
                    List.of(new Allergen(allergenId, "Gluten", AllergenSeverity.HIGH)),
                    List.of(), now, now
            );

            when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(existingEntity));
            when(allergenRepository.findAllById(List.of(allergenId))).thenReturn(List.of(allergenEntity));
            when(ingredientRepository.findByNameIn(any())).thenReturn(List.of());
            when(recipeRepository.save(any(RecipeEntity.class))).thenReturn(existingEntity);

            var result = adapter.update(recipeId, updatedRecipe);

            assertNotNull(result);
            verify(recipeAllergenRepository).deleteByRecipeId(recipeId);
            verify(allergenRepository).findAllById(List.of(allergenId));
        }

        @Test
        void shouldPreserveParentIdWhenUpdating() {
            var recipeId = UUID.randomUUID();
            var parentId = UUID.randomUUID();
            var existingEntity = createRecipeEntity(recipeId, "Variant", RecipeStatus.DRAFT);
            var parentEntity = createRecipeEntity(parentId, "Parent", RecipeStatus.PUBLISHED);
            existingEntity.setParent(parentEntity);

            var updatedRecipe = new Recipe(
                    recipeId, "Updated Variant", "New summary", parentId, "chef_test",
                    30, null, RecipeStatus.DRAFT, List.of(), List.of(), List.of(), List.of(), now, now
            );

            when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(existingEntity));
            when(allergenRepository.findAllById(any())).thenReturn(List.of());
            when(ingredientRepository.findByNameIn(any())).thenReturn(List.of());
            when(recipeRepository.save(any(RecipeEntity.class))).thenReturn(existingEntity);

            var result = adapter.update(recipeId, updatedRecipe);

            assertNotNull(result);
            assertEquals(parentId, result.parentId());
        }

        @Test
        void shouldDeleteOldRelationsBeforeUpdating() {
            var recipeId = UUID.randomUUID();
            var existingEntity = createRecipeEntity(recipeId, "Recipe", RecipeStatus.DRAFT);
            var updatedRecipe = createRecipe(recipeId, "Updated", RecipeStatus.DRAFT);

            when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(existingEntity));
            when(allergenRepository.findAllById(any())).thenReturn(List.of());
            when(ingredientRepository.findByNameIn(any())).thenReturn(List.of());
            when(dietaryRepository.findByNameIn(any())).thenReturn(List.of());
            when(recipeRepository.save(any(RecipeEntity.class))).thenReturn(existingEntity);

            adapter.update(recipeId, updatedRecipe);

            var inOrder = inOrder(recipeAllergenRepository, recipeIngredientRepository, recipeDietaryRepository, entityManager, recipeRepository);
            inOrder.verify(recipeAllergenRepository).deleteByRecipeId(recipeId);
            inOrder.verify(recipeIngredientRepository).deleteByRecipeId(recipeId);
            inOrder.verify(recipeDietaryRepository).deleteByRecipeId(recipeId);
            inOrder.verify(entityManager).flush();
            inOrder.verify(entityManager).clear();
            inOrder.verify(recipeRepository).findById(recipeId);
            inOrder.verify(recipeRepository).save(any(RecipeEntity.class));
        }

        @Test
        void shouldUpdateStepsCorrectly() {
            var recipeId = UUID.randomUUID();
            var existingEntity = createRecipeEntity(recipeId, "Recipe", RecipeStatus.DRAFT);

            var updatedRecipe = new Recipe(
                    recipeId, "Recipe", "Summary", null, "chef_test",
                    30, null, RecipeStatus.DRAFT,
                    List.of(
                            new fr.uge.forkeat.service.model.recipe.RecipeStep(1, "Step 1"),
                            new fr.uge.forkeat.service.model.recipe.RecipeStep(2, "Step 2"),
                            new fr.uge.forkeat.service.model.recipe.RecipeStep(3, "Step 3")
                    ),
                    List.of(), List.of(), List.of(), now, now
            );

            when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(existingEntity));
            when(allergenRepository.findAllById(any())).thenReturn(List.of());
            when(ingredientRepository.findByNameIn(any())).thenReturn(List.of());
            when(recipeRepository.save(any(RecipeEntity.class))).thenReturn(existingEntity);

            var result = adapter.update(recipeId, updatedRecipe);

            assertNotNull(result);
            assertEquals(3, result.stepByStepInstructions().size());
        }
    }

    @Nested
    class SuperLike {

        @Test
        void shouldSaveSuperLike() {
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            long amount = 100L;

            adapter.superLikeRecipe(userId, recipeId, amount, null, false);

            verify(superLikeRepository).save(any(SuperLikeEntity.class));
        }

        @Test
        void shouldReturnTrueWhenUserHasSuperLiked() {
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            when(superLikeRepository.existsByRecipeIdAndUserId(userId, recipeId)).thenReturn(true);

            assertTrue(adapter.hasSuperLikedRecipe(userId, recipeId));
            verify(superLikeRepository).existsByRecipeIdAndUserId(userId, recipeId);
        }

        @Test
        void shouldReturnFalseWhenUserHasNotSuperLiked() {
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            when(superLikeRepository.existsByRecipeIdAndUserId(userId, recipeId)).thenReturn(false);

            assertFalse(adapter.hasSuperLikedRecipe(userId, recipeId));
            verify(superLikeRepository).existsByRecipeIdAndUserId(userId, recipeId);
        }

        @Test
        void shouldThrowWhenUserIdIsNullForHasSuperLiked() {
            assertThrows(NullPointerException.class, () -> adapter.hasSuperLikedRecipe(null, UUID.randomUUID()));
        }

        @Test
        void shouldThrowWhenRecipeIdIsNullForHasSuperLiked() {
            assertThrows(NullPointerException.class, () -> adapter.hasSuperLikedRecipe(UUID.randomUUID(), null));
        }
    }

    private RecipeEntity createRecipeEntity(UUID id, String title, RecipeStatus status) {
        var entity = new RecipeEntity();
        entity.setId(id);
        entity.setTitle(title);
        entity.setSummary("Summary for " + title);
        entity.setAuthor(author);
        entity.setStatus(status);
        entity.setPreparationMinutes(30);
        entity.setStepByStepInstructions(List.of(new fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeStep(1, "Default step")));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private Recipe createRecipe(UUID id, String title, RecipeStatus status) {
        return new Recipe(
                id, title, "Summary for " + title, null, "chef_test",
                30, null, status, List.of(), List.of(), List.of(), List.of(), now, now
        );
    }

}
