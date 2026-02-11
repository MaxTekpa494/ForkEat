package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.AllergenEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.IngredientEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.AllergenRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.IngredientRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Allergen;
import fr.uge.forkeat.service.model.recipe.AllergenSeverity;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeIngredient;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipePersistenceAdapterTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AllergenRepository allergenRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    private RecipePersistenceAdapter adapter;
    private UserEntity author;
    private Instant now;

    @BeforeEach
    void setUp() {
        adapter = new RecipePersistenceAdapter(recipeRepository, userRepository, allergenRepository, ingredientRepository);
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

    @Test
    void constructor_shouldThrowWhenRecipeRepositoryIsNull() {
        assertThrows(NullPointerException.class, () ->
                new RecipePersistenceAdapter(null, userRepository, allergenRepository, ingredientRepository));
    }

    @Test
    void constructor_shouldThrowWhenUserRepositoryIsNull() {
        assertThrows(NullPointerException.class, () ->
                new RecipePersistenceAdapter(recipeRepository, null, allergenRepository, ingredientRepository));
    }

    @Test
    void constructor_shouldThrowWhenAllergenRepositoryIsNull() {
        assertThrows(NullPointerException.class, () ->
                new RecipePersistenceAdapter(recipeRepository, userRepository, null, ingredientRepository));
    }

    @Test
    void constructor_shouldThrowWhenIngredientRepositoryIsNull() {
        assertThrows(NullPointerException.class, () ->
                new RecipePersistenceAdapter(recipeRepository, userRepository, allergenRepository, null));
    }

    @Test
    void findById_shouldReturnRecipeWhenFound() {
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
    void findById_shouldReturnEmptyWhenNotFound() {
        var recipeId = UUID.randomUUID();
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

        var result = adapter.findById(recipeId);

        assertTrue(result.isEmpty());
        verify(recipeRepository).findById(recipeId);
    }

    @Test
    void findById_shouldThrowWhenIdIsNull() {
        assertThrows(NullPointerException.class, () -> adapter.findById(null));
    }

    @Test
    void findByStatus_shouldReturnMatchingRecipes() {
        var entity1 = createRecipeEntity(UUID.randomUUID(), "Recette 1", RecipeStatus.PUBLISHED);
        var entity2 = createRecipeEntity(UUID.randomUUID(), "Recette 2", RecipeStatus.PUBLISHED);
        when(recipeRepository.findByStatus(RecipeStatus.PUBLISHED)).thenReturn(List.of(entity1, entity2));

        var result = adapter.findByStatus("PUBLISHED");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.status() == RecipeStatus.PUBLISHED));
        verify(recipeRepository).findByStatus(RecipeStatus.PUBLISHED);
    }

    @Test
    void findByStatus_shouldReturnEmptyListWhenNoMatch() {
        when(recipeRepository.findByStatus(RecipeStatus.DRAFT)).thenReturn(List.of());

        var result = adapter.findByStatus("DRAFT");

        assertTrue(result.isEmpty());
        verify(recipeRepository).findByStatus(RecipeStatus.DRAFT);
    }

    @Test
    void findByAuthorId_shouldReturnAuthorRecipes() {
        var authorId = author.getId();
        var entity = createRecipeEntity(UUID.randomUUID(), "Ma recette", RecipeStatus.DRAFT);
        when(recipeRepository.findByAuthorId(authorId)).thenReturn(List.of(entity));

        var result = adapter.findByAuthorId(authorId);

        assertEquals(1, result.size());
        assertEquals("Ma recette", result.getFirst().title());
        verify(recipeRepository).findByAuthorId(authorId);
    }

    @Test
    void findByAuthorId_shouldThrowWhenAuthorIdIsNull() {
        assertThrows(NullPointerException.class, () -> adapter.findByAuthorId(null));
    }

    @Test
    void findByAuthorId_shouldReturnEmptyListWhenAuthorHasNoRecipes() {
        var authorId = UUID.randomUUID();
        when(recipeRepository.findByAuthorId(authorId)).thenReturn(List.of());

        var result = adapter.findByAuthorId(authorId);

        assertTrue(result.isEmpty());
    }

    @Test
    void save_shouldPersistRecipeAndReturnIt() {
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
    void save_shouldThrowWhenRecipeIsNull() {
        assertThrows(NullPointerException.class, () -> adapter.save(null));
    }

    @Test
    void save_shouldThrowWhenUserNotFound() {
        var recipe = createRecipe(UUID.randomUUID(), "Test", RecipeStatus.DRAFT);
        when(userRepository.findByUsername("chef_test")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> adapter.save(recipe));
    }

    @Test
    void save_shouldHandleRecipeWithParent() {
        var parentId = UUID.randomUUID();
        var recipeId = UUID.randomUUID();
        var parentEntity = createRecipeEntity(parentId, "Parent", RecipeStatus.PUBLISHED);
        var recipe = new Recipe(
                recipeId, "Variante", "Summary", parentId, "chef_test",
                30, null, RecipeStatus.DRAFT, List.of(), List.of(), List.of(), Map.of(), now, now
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
    void save_shouldThrowWhenParentNotFound() {
        var parentId = UUID.randomUUID();
        var recipe = new Recipe(
                UUID.randomUUID(), "Variante", "Summary", parentId, "chef_test",
                30, null, RecipeStatus.DRAFT, List.of(), List.of(), List.of(), Map.of(), now, now
        );

        when(userRepository.findByUsername("chef_test")).thenReturn(Optional.of(author));
        when(recipeRepository.findById(parentId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> adapter.save(recipe));
    }

    @Test
    void save_shouldHandleRecipeWithAllergens() {
        var recipeId = UUID.randomUUID();
        var allergenId = UUID.randomUUID();
        var allergenEntity = new AllergenEntity("Gluten", AllergenSeverity.HIGH);
        allergenEntity.setId(allergenId);

        var recipe = new Recipe(
                recipeId, "Recette avec allergene", "Summary", null, "chef_test",
                30, null, RecipeStatus.DRAFT, List.of(), List.of(),
                List.of(new Allergen(allergenId, "Gluten", AllergenSeverity.HIGH)), Map.of(), now, now
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
    void save_shouldHandleRecipeWithIngredients() {
        var recipeId = UUID.randomUUID();
        var ingredientEntity = new IngredientEntity("Farine", "Cereale", false);
        ingredientEntity.setId(UUID.randomUUID());

        var recipe = new Recipe(
                recipeId, "Recette avec ingredient", "Summary", null, "chef_test",
                30, null, RecipeStatus.DRAFT, List.of(),
                List.of(new RecipeIngredient("Farine", 250.0, "g")),
                List.of(), Map.of(), now, now
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

    @Nested
    class FullTextSearch {
        @Test
        void findByStatusAndSearch_shouldDelegateToRepository() {
            var entity = createRecipeEntity(UUID.randomUUID(), "Tarte aux pommes", RecipeStatus.PUBLISHED);
            var page = new PageImpl<>(List.of(entity), PageRequest.of(0, 12), 1);
            when(recipeRepository.findByStatusAndFullTextSearch(RecipeStatus.PUBLISHED, "tarte", PageRequest.of(0, 12)))
                    .thenReturn(page);

            var result = adapter.findByStatusAndSearch("PUBLISHED", "tarte", 12, 0);

            assertEquals(1, result.items().size());
            assertEquals("Tarte aux pommes", result.items().getFirst().title());
            assertEquals(1L, result.total());
            verify(recipeRepository).findByStatusAndFullTextSearch(RecipeStatus.PUBLISHED, "tarte", PageRequest.of(0, 12));
        }

        @Test
        void findByStatusAndSearch_shouldThrowWhenStatusIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.findByStatusAndSearch(null, "tarte", 12, 0));
        }

        @Test
        void findByStatusAndSearch_shouldThrowWhenSearchIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.findByStatusAndSearch("PUBLISHED", null, 12, 0));
        }

        @Test
        void findByStatusAndSearch_shouldMapEntitiesToDomain() {
            var entity1 = createRecipeEntity(UUID.randomUUID(), "Recette 1", RecipeStatus.PUBLISHED);
            var entity2 = createRecipeEntity(UUID.randomUUID(), "Recette 2", RecipeStatus.PUBLISHED);
            var page = new PageImpl<>(List.of(entity1, entity2), PageRequest.of(0, 12), 2);
            when(recipeRepository.findByStatusAndFullTextSearch(RecipeStatus.PUBLISHED, "recette", PageRequest.of(0, 12)))
                    .thenReturn(page);

            var result = adapter.findByStatusAndSearch("PUBLISHED", "recette", 12, 0);

            assertEquals(2, result.items().size());
            assertTrue(result.items().stream().allMatch(r -> r.status() == RecipeStatus.PUBLISHED));
        }

        @Test
        void findByStatusAndSearchAndAllergens_shouldDelegateToRepository() {
            var entity = createRecipeEntity(UUID.randomUUID(), "Salade", RecipeStatus.PUBLISHED);
            var allergens = List.of("Gluten");
            var page = new PageImpl<>(List.of(entity), PageRequest.of(0, 12), 1);
            when(recipeRepository.findByStatusAndSearchAndAllergens(RecipeStatus.PUBLISHED, "salade", allergens, PageRequest.of(0, 12)))
                    .thenReturn(page);

            var result = adapter.findByStatusAndSearchAndAllergens("PUBLISHED", "salade", allergens, 12, 0);

            assertEquals(1, result.items().size());
            assertEquals(1L, result.total());
            verify(recipeRepository).findByStatusAndSearchAndAllergens(RecipeStatus.PUBLISHED, "salade", allergens, PageRequest.of(0, 12));
        }

        @Test
        void findByStatusAndSearchAndAllergens_shouldThrowWhenStatusIsNull() {
            assertThrows(NullPointerException.class,
                    () -> adapter.findByStatusAndSearchAndAllergens(null, "test", List.of("Gluten"), 12, 0));
        }

        @Test
        void findByStatusAndSearchAndAllergens_shouldThrowWhenAllergensIsNull() {
            assertThrows(NullPointerException.class,
                    () -> adapter.findByStatusAndSearchAndAllergens("PUBLISHED", "test", null, 12, 0));
        }
    }

    @Test
    void findAllAllergens_shouldReturnMappedAllergens() {
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
    void findAllAllergens_shouldReturnEmptyListWhenNoAllergens() {
        when(allergenRepository.findAll()).thenReturn(List.of());

        var result = adapter.findAllAllergens();

        assertTrue(result.isEmpty());
        verify(allergenRepository).findAll();
    }

    @Test
    void deleteById_shouldCallRepositoryDelete() {
        var recipeId = UUID.randomUUID();

        adapter.deleteById(recipeId);

        verify(recipeRepository).deleteById(recipeId);
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
        entity.setDietaryFlag(Map.of());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private Recipe createRecipe(UUID id, String title, RecipeStatus status) {
        return new Recipe(
                id, title, "Summary for " + title, null, "chef_test",
                30, null, status, List.of(), List.of(), List.of(), Map.of(), now, now
        );
    }
}
