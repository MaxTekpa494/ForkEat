package fr.uge.forkeat.infrastructure.security;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.presentation.dto.user.UserLoginDTO;
import fr.uge.forkeat.presentation.dto.user.UserRegisterDTO;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class SecurityTests extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    public void authenticationTest() throws Exception {
        // Create admin user manually
        var admin = new UserEntity();
        admin.setUsername("admin1");
        admin.setFirstName("Admin");
        admin.setLastName("System");
        admin.setEmail("admin@forkeat.app");
        admin.setPassword(passwordEncoder.encode("admin"));
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setAuthMode(AuthMode.LOCAL);
        admin.setEmailVerified(true);
        userRepository.save(admin);

        var user = new UserRegisterDTO("S1dAli", "SidAli", "Cherrati", "Password1", "sidali@gmail.com");

        var userLogin = new UserLoginDTO("S1dAli", "Password1");


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

        var json = objectMapper.readTree(stringResponse);

        //Store the token
        var tokenUser = "Bearer " + (json.get("resource").get("token").asText());


        var adminDTO = new UserLoginDTO("admin", "admin");
        //We try to recup the adminToken
        var loginAdminBodyResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminDTO)))
                .andExpect(status().isOk()).andReturn();

        JsonNode adminLoginJson = objectMapper.readTree(loginAdminBodyResponse.getResponse().getContentAsString());

        var tokenAdmin = "Bearer " + (adminLoginJson.get("resource").get("token").asText());


        //We try to create a moderator
        var moderatorDTO = new UserRegisterDTO("modo", "Max", "Tekpa", "Password1", "modo@gmail.com");
        mockMvc.perform(post("/api/admin/register")
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moderatorDTO)))
                .andExpect(status().isCreated());
    }
}
