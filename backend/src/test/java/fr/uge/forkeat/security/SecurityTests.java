package fr.uge.forkeat.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.uge.forkeat.presentation.rest.dto.UserLogin;
import fr.uge.forkeat.presentation.rest.dto.UserRegister;
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
        var user = new UserRegister("S1dAli", "SidAli", "Cherrati", "password1", "sidali@gmail.com");

        var userLogin = new UserLogin("S1dAli", "password1");


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


        //We test the endpoint is reachable by an authenticated User
        mockMvc.perform(get("/api/user/greeting")
                        .header("Authorization", tokenUser))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/greeting")
                        .header("Authorization", tokenUser))
                .andExpect(status().isForbidden());

        //We test the endpoint is not reachable by an non-authenticated User
        mockMvc.perform(get("/api/user/greeting"))
                .andExpect(status().isUnauthorized());


        var adminDTO = new UserLogin("admin", "admin");
        //We try to recup the adminToken
        var loginAdminBodyResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminDTO)))
                .andExpect(status().isOk()).andReturn();

        JsonNode adminLoginJson = objectMapper.readTree(loginAdminBodyResponse.getResponse().getContentAsString());

        var tokenAdmin = "Bearer " + (adminLoginJson.get("token").asString());

        //We try the different endpoints as an admin

        mockMvc.perform(get("/api/admin/greeting")
                        .header("Authorization", tokenAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/moderator/greeting")
                        .header("Authorization", tokenAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/greeting")
                        .header("Authorization", tokenAdmin))
                .andExpect(status().isOk());



        //We try to create a moderator

        var moderatorDTO = new UserRegister("modo", "Max", "Tekpa", "password1", "modo@gmail.com");
        mockMvc.perform(post("/api/admin/register")
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moderatorDTO)))
                .andExpect(status().isOk());

        //We try to authentify as the newly created moderator

        var moderatorLoginDTO = new UserLogin("modo", "password1");
        var loginModeratorBodyResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moderatorLoginDTO)))
                .andExpect(status().isOk()).andReturn();

        JsonNode moderatorLoginJson = objectMapper.readTree(loginModeratorBodyResponse.getResponse().getContentAsString());

        var tokenModerator = "Bearer " + (moderatorLoginJson.get("token").asString());


        //We try the different endpoints as a Moderator

        mockMvc.perform(get("/api/admin/greeting")
                        .header("Authorization", tokenModerator))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/moderator/greeting")
                        .header("Authorization", tokenModerator))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/greeting")
                        .header("Authorization", tokenModerator))
                .andExpect(status().isOk());

    }
}
