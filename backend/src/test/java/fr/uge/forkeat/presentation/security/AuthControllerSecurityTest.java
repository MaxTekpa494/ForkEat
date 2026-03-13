package fr.uge.forkeat.presentation.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.infrastructure.config.RateLimitFilter;
import fr.uge.forkeat.presentation.dto.user.UserRegisterDTO;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;


@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@Transactional
public class AuthControllerSecurityTest extends AbstractIntegrationTest {


    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    @MockitoBean
    private AuthenticationManager authenticationManager;


    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private GoogleTokenVerificationService googleTokenVerificationService;

    @MockitoBean
    private UserUpdateService userUpdateService;

    @MockitoBean
    RateLimitFilter rateLimitingFilter;


    @BeforeEach
    void setup() {

        var mapper = new ObjectMapper();

        when(userRegistrationService.registerUser(any())).thenReturn(createUser(UUID.randomUUID()));

        when(authPort.generateToken(any())).thenReturn("TOKEN");
        when(authPort.extractUsername()).thenReturn("Pid3ALI");

        when(userService.getUserByUsername("Pid3ALI")).thenReturn(createUser(UUID.randomUUID()));
        when(userService.findByEmail("sid@gmail.com")).thenReturn(Optional.of(createUser(UUID.randomUUID())));

        var mockGoogleToken = mock(GoogleIdToken.class);
        var mockPayload = mock(GoogleIdToken.Payload.class);
        when(mockGoogleToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getEmail()).thenReturn("sid@gmail.com");
        when(mockPayload.get("given_name")).thenReturn("John");
        when(mockPayload.get("family_name")).thenReturn("Doe");
        when(googleTokenVerificationService.verify(any())).thenReturn(mockGoogleToken);
        when(userRegistrationService.registerUserFromOAuth2(any(), any(), any(), any())).thenReturn(createUser(UUID.randomUUID()));

        doNothing().when(emailVerificationService).sendPasswordChangeCode(any(), any());
        doNothing().when(userUpdateService).confirmForgotPasswordChange(any(), any(), any(), any());
    }

    @Nested
    class AuthRestControllerSecurityTest{
        @Test
        void testRegister() throws Exception {
            var mapper = new ObjectMapper();

            testRights(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(new UserRegisterDTO("aza", "adel", "ziani", "beaugossedu77", "adel@forkeat.com"))), AuthorizationTest.UNAUTHENTICATED);
        }

        @Test
        void testLogin() throws Exception {
            testRights(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                {"username": "testuser", "password": "password123"}
            """), AuthorizationTest.UNAUTHENTICATED);
        }

        @Test
        void testMe() throws Exception {
            testRights(get("/api/auth/me"), AuthorizationTest.MEMBER);
        }

        @Test
        void testGoogleLogin() throws Exception {
            testRights(post("/api/auth/google-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                {"idToken": "google.id.token"}
            """), AuthorizationTest.UNAUTHENTICATED);
        }

        @Test
        void testForgotPassword() throws Exception {
            testRights(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                {"email": "test@example.com"}
            """), AuthorizationTest.UNAUTHENTICATED);
        }

        @Test
        void testForgotPasswordConfirmCode() throws Exception {
            testRights(post("/api/auth/forgot-password/confirm-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                {"email": "test@example.com", "code": "123456", "password": "newpass123", "confirmPassword": "newpass123"}
            """), AuthorizationTest.UNAUTHENTICATED);
        }
    }


    private void testRights(MockHttpServletRequestBuilder requestBuilders, AuthorizationTest authorization) throws Exception {
        var expected = status().isForbidden();

        var expectedForUnauthenticated = authorization.equals(AuthorizationTest.UNAUTHENTICATED) ? status().is2xxSuccessful() : status().isUnauthorized();
        mockMvc.perform(requestBuilders).andExpect(expectedForUnauthenticated);

        if(authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))).andExpect(expected);

        if(authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);


        if(authorization.equals(AuthorizationTest.MODERATOR)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

        if(authorization.equals(AuthorizationTest.ADMIN)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);
    }

    private User createUser(UUID id) {
        return new User(id, "PaxGPT", "Pax", "Pekpa", "a@gmail.com",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, null, null, true);
    }
}
