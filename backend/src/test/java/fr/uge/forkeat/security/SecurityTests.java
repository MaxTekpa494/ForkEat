package fr.uge.forkeat.security;

import fr.uge.forkeat.presentation.dto.user.UserLoginDTO;
import fr.uge.forkeat.presentation.dto.user.UserRegisterDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class SecurityTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("forkeat_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Ajouter stringtype=unspecified pour que PostgreSQL gère les ENUMs
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "&stringtype=unspecified");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;



    @Test
    public void authenticationTest() throws Exception {
        var user = new UserRegisterDTO("S1dAli", "SidAli", "Cherrati", "password1", "sidali@gmail.com");

        var userLogin = new UserLoginDTO("S1dAli", "password1");


        var objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(user));

        //Testing creation of a new user
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());

        //Testing to authentify the newly created user
        var bodyResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk()).andReturn();

        var stringResponse = bodyResponse.getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(stringResponse);

        //Store the token
        var tokenUser = "Bearer " + (json.get("token").asString());


        var adminDTO = new UserLoginDTO("admin", "admin");
        //We try to recup the adminToken
        var loginAdminBodyResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminDTO)))
                .andExpect(status().isOk()).andReturn();

        JsonNode adminLoginJson = objectMapper.readTree(loginAdminBodyResponse.getResponse().getContentAsString());

        var tokenAdmin = "Bearer " + (adminLoginJson.get("token").asString());


        //We try to create a moderator
        var moderatorDTO = new UserRegisterDTO("modo", "Max", "Tekpa", "password1", "modo@gmail.com");
        mockMvc.perform(post("/api/admin/register")
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moderatorDTO)))
                .andExpect(status().isOk());
    }
}
