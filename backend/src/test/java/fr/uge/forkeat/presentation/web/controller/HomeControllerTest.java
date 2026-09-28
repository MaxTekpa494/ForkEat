package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeWebController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerTest {

    private final MockMvc mockMvc;


    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private RecipeService recipeService;

    @Autowired
    public HomeControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() {
        when(recipeService.getTopLikedRecipe()).thenReturn(Optional.empty());
    }

    @Test
    void home_ShouldReturnHomeView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/index"))
                .andExpect(model().attribute("pageTitle", "Accueil - ForkEat"));
    }

    @Test
    void home_ShouldContainStatistics() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("totalRecipes"))
                .andExpect(model().attributeExists("totalUsers"))
                .andExpect(model().attributeExists("totalChefs"));
    }

    @Test
    void home_ShouldReturnExpectedStatValues() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalRecipes", 1250))
                .andExpect(model().attribute("totalUsers", 8500))
                .andExpect(model().attribute("totalChefs", 450));
    }
}
