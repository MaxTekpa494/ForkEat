package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.config.JwtFilter;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecipeWebController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecipeWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;


    @Test
    @WithMockUser
    void listRecipes_shouldReturnIndexViewWithDefaultParams() throws Exception {
        var recipe = createRecipe("Tarte aux pommes", RecipeStatus.PUBLISHED);
        var pageResult = new PageResult<>(List.of(recipe), 1L);

        when(recipeService.findByStatus("PUBLISHED", 12, 0)).thenReturn(pageResult);

        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/index"))
                .andExpect(model().attributeExists("recipes"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 1))
                .andExpect(model().attribute("totalRecipes", 1L));
    }

    @Test
    @WithMockUser
    void listRecipes_shouldPassCustomStatusAndPagination() throws Exception {
        var recipe = createRecipe("Brouillon", RecipeStatus.DRAFT);
        var pageResult = new PageResult<>(List.of(recipe), 1L);

        when(recipeService.findByStatus("DRAFT", 5, 2)).thenReturn(pageResult);

        mockMvc.perform(get("/recipes")
                        .param("status", "DRAFT")
                        .param("size", "5")
                        .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/index"))
                .andExpect(model().attribute("currentPage", 2))
                .andExpect(model().attribute("totalPages", 1));
    }

    @Test
    @WithMockUser
    void listRecipes_shouldCalculateTotalPagesCorrectly() throws Exception {
        var recipes = List.of(
                createRecipe("Recipe 1", RecipeStatus.PUBLISHED),
                createRecipe("Recipe 2", RecipeStatus.PUBLISHED)
        );
        var pageResult = new PageResult<>(recipes, 5L);

        when(recipeService.findByStatus("PUBLISHED", 2, 0)).thenReturn(pageResult);

        mockMvc.perform(get("/recipes")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalPages", 3))
                .andExpect(model().attribute("totalRecipes", 5L));
    }

    @Test
    @WithMockUser
    void listRecipes_shouldPassSearchParam() throws Exception {
        var pageResult = new PageResult<>(List.<Recipe>of(), 0L);

        when(recipeService.findByStatus("PUBLISHED", 12, 0)).thenReturn(pageResult);

        mockMvc.perform(get("/recipes")
                        .param("search", "tarte"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("search", "tarte"));
    }

    @Test
    @WithMockUser
    void listRecipes_shouldReturnEmptyListWhenNoRecipes() throws Exception {
        var pageResult = new PageResult<>(List.<Recipe>of(), 0L);

        when(recipeService.findByStatus("PUBLISHED", 12, 0)).thenReturn(pageResult);

        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/index"))
                .andExpect(model().attribute("totalRecipes", 0L))
                .andExpect(model().attribute("totalPages", 0));
    }


    @Test
    @WithMockUser
    void viewRecipe_shouldReturnDetailViewWhenRecipeExists() throws Exception {
        var id = UUID.randomUUID();
        var recipe = createRecipeWithId(id, "Quiche Lorraine", RecipeStatus.PUBLISHED, null);

        when(recipeService.findById(id)).thenReturn(recipe);

        mockMvc.perform(get("/recipes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/detail"))
                .andExpect(model().attributeExists("recipe"));
    }

    @Test
    @WithMockUser
    void viewRecipe_shouldIncludeParentWhenRecipeIsVariant() throws Exception {
        var parentId = UUID.randomUUID();
        var variantId = UUID.randomUUID();
        var parent = createRecipeWithId(parentId, "Recette originale", RecipeStatus.PUBLISHED, null);
        var variant = createRecipeWithId(variantId, "Variante", RecipeStatus.PUBLISHED, parentId);

        when(recipeService.findById(variantId)).thenReturn(variant);
        when(recipeService.findById(parentId)).thenReturn(parent);

        mockMvc.perform(get("/recipes/{id}", variantId))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/detail"))
                .andExpect(model().attributeExists("recipe"))
                .andExpect(model().attributeExists("parent"));
    }

    @Test
    @WithMockUser
    void viewRecipe_shouldNotIncludeParentWhenRecipeIsNotVariant() throws Exception {
        var id = UUID.randomUUID();
        var recipe = createRecipeWithId(id, "Tarte classique", RecipeStatus.PUBLISHED, null);

        when(recipeService.findById(id)).thenReturn(recipe);

        mockMvc.perform(get("/recipes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("parent"));
    }

    @Test
    @WithMockUser
    void viewRecipe_shouldReturn404WhenRecipeNotFound() throws Exception {
        var id = UUID.randomUUID();

        when(recipeService.findById(id)).thenThrow(new RecipeNotFoundException(id));

        mockMvc.perform(get("/recipes/{id}", id))
                .andExpect(status().isNotFound());
    }


    private Recipe createRecipe(String title, RecipeStatus status) {
        return createRecipeWithId(UUID.randomUUID(), title, status, null);
    }

    private Recipe createRecipeWithId(UUID id, String title, RecipeStatus status, UUID parentId) {
        return new Recipe(
                id,
                title,
                "Summary for " + title,
                parentId,
                "chef_test",
                30,
                null,
                status,
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                null,
                null
        );
    }
}
