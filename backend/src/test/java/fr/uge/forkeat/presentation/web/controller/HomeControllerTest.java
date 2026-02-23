package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeWebController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerTest {

    private final MockMvc mockMvc;


    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    public HomeControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
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
