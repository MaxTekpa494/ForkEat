package fr.uge.forkeat.presentation.security;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.service.ProfileService;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.model.user.projection.UserAccountDetails;
import fr.uge.forkeat.service.model.user.projection.UserSocialStats;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserService;
import fr.uge.forkeat.service.user.UserUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.springframework.http.MediaType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;


@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@Transactional
public class AccountControllerSecurityTest extends AbstractIntegrationTest {


    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserUpdateService userUpdateService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private ProfileService profileService;

    @BeforeEach
    void setup() {

        var mockUser = createUser(UUID.randomUUID());
        var mockUpdatedUser = mockUser;

        when(authPort.extractUsername()).thenReturn("PAX");
        when(userService.getUserByUsername("PAX")).thenReturn(mockUser);

        when(userUpdateService.updateProfile(any(), any(), any(), any())).thenReturn(mockUpdatedUser);

        doNothing().when(userUpdateService).requestPasswordChange(any(), any(), any(), any());
        doNothing().when(emailVerificationService).confirmPasswordChange(any(), any());

        doNothing().when(userUpdateService).requestEmailChange(any(), any(), any(), any(), any());
        when(emailVerificationService.confirmEmailChange(any(), any())).thenReturn(mockUpdatedUser);

        doNothing().when(emailVerificationService).sendEmailConfirmation(any(), any());
        doNothing().when(userUpdateService).setPasswordForOAuthUser(any(), any(), any());

        var mockAccountVm = createUserAccountDetails(); // adapte selon ton type de retour
        when(profileService.getAccountDetails(any())).thenReturn(mockAccountVm);
        doNothing().when(authPort).refreshAuthentication(any());
    }

    @Nested
    class AccountRestControllerSecurityTest{
        @Test
        void testGetAccount() throws Exception {
            testRights(get("/api/account"), AuthorizationTest.MEMBER);
        }

        @Test
        void testUpdateProfile() throws Exception {
            testRights(put("/api/account")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                {"username": "newusername", "firstName": "John", "lastName": "Doe"}
            """), AuthorizationTest.MEMBER);
        }

        @Test
        void testRequestPasswordChange() throws Exception {
            testRights(post("/api/account/password-change-requests")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                {"currentPassword": "oldpass", "newPassword": "newpass123", "confirmPassword": "newpass123"}
            """), AuthorizationTest.MEMBER);
        }

        @Test
        void testConfirmPasswordChange() throws Exception {
            testRights(put("/api/account/password-change-requests")
                    .param("code", "123456"), AuthorizationTest.MEMBER);
        }

        @Test
        void testRequestEmailChange() throws Exception {
            testRights(post("/api/account/email-change-requests")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                {"newEmail": "new@example.com", "currentPassword": "pass123", "newPassword": "newpass123", "confirmPassword": "newpass123"}
            """), AuthorizationTest.MEMBER);
        }

        @Test
        void testConfirmEmailChange() throws Exception {
            testRights(put("/api/account/email-change-requests")
                    .param("code", "123456"), AuthorizationTest.MEMBER);
        }

        @Test
        void testResendConfirmation() throws Exception {
            testRights(post("/api/account/email-confirmations"), AuthorizationTest.MEMBER);
        }

        @Test
        void testSetPasswordForOAuthUser() throws Exception {
            testRights(put("/api/account/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                {"newPassword": "newpass123", "confirmPassword": "newpass123"}
            """), AuthorizationTest.MEMBER);
        }
    }

    @Nested
    class AccountWebControllerSecurityTest {

        @Test
        void testGetAccount() throws Exception {
            testRightsMVCNoRedirect(get("/account"), AuthorizationTest.MEMBER);
        }

        @Test
        void testUpdateProfile() throws Exception {
            testRightsMVC(post("/account")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("username", "newusername")
                    .param("firstName", "John")
                    .param("lastName", "Doe"), AuthorizationTest.MEMBER);
        }

        @Test
        void testRequestPasswordChange() throws Exception {
            testRightsMVC(post("/account/password-change-requests")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("currentPassword", "oldpass")
                    .param("newPassword", "newpass123")
                    .param("confirmPassword", "newpass123"), AuthorizationTest.MEMBER);
        }

        @Test
        void testConfirmActionPage() throws Exception {
            testRightsMVCNoRedirect(get("/account/confirm-action")
                    .param("actionType", "PASSWORD_CHANGE"), AuthorizationTest.MEMBER);
        }

        @Test
        void testConfirmPasswordChange() throws Exception {
            testRightsMVC(post("/account/password-change-requests/confirm")
                    .param("code", "123456"), AuthorizationTest.MEMBER);
        }

        @Test
        void testRequestEmailChange() throws Exception {
            testRightsMVC(post("/account/email-change-requests")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("newEmail", "new@example.com")
                    .param("currentPassword", "pass123")
                    .param("newPassword", "newpass123")
                    .param("confirmPassword", "newpass123"), AuthorizationTest.MEMBER);
        }

        @Test
        void testConfirmEmailChange() throws Exception {
            testRightsMVC(post("/account/email-change-requests/confirm")
                    .param("code", "123456"), AuthorizationTest.MEMBER);
        }

        @Test
        void testResendConfirmation() throws Exception {
            testRightsMVC(post("/account/email-confirmations"), AuthorizationTest.MEMBER);
        }

        @Test
        void testSetPasswordForOAuthUser() throws Exception {
            testRightsMVC(post("/account/password")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("newPassword", "newpass123")
                    .param("confirmPassword", "newpass123"), AuthorizationTest.MEMBER);
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


    private void testRightsMVC(MockHttpServletRequestBuilder requestBuilders, AuthorizationTest authorization) throws Exception {
        var expectedBlocked = status().is3xxRedirection();
        var expectedSuccess = status().is3xxRedirection(); // POST redirige vers /account en cas de succès

        // Non authentifié → toujours redirigé (vers /login ou succès si UNAUTHENTICATED)
        mockMvc.perform(requestBuilders)
                .andExpect(expectedBlocked);

        var expected = authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)
                ? expectedSuccess : expectedBlocked;

        mockMvc.perform(requestBuilders.with(user("PAX").roles("MEMBER")))
                .andExpect(expected);

        if (authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
            expected = expectedSuccess;
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                        .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED"))))
                .andExpect(expected);

        if (authorization.equals(AuthorizationTest.MODERATOR)) {
            expected = expectedSuccess;
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                        .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED"))))
                .andExpect(expected);

        if (authorization.equals(AuthorizationTest.ADMIN)) {
            expected = expectedSuccess;
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED"))))
                .andExpect(expected);
    }


    private void testRightsMVCNoRedirect(MockHttpServletRequestBuilder requestBuilders, AuthorizationTest authorization) throws Exception {
        var expected = status().is3xxRedirection();

        var expectedForUnauthenticated = authorization.equals(AuthorizationTest.UNAUTHENTICATED) ? status().is2xxSuccessful() : status().is3xxRedirection();
        mockMvc.perform(requestBuilders).andExpect(expectedForUnauthenticated);

        if(authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))).andExpect(expected);

        if(authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
            expected = status().is2xxSuccessful();
        }

        mockMvc.perform(requestBuilders.with(user("PAX")
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);


        if(authorization.equals(AuthorizationTest.MODERATOR)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

        if(authorization.equals(AuthorizationTest.ADMIN)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);
    }


    private UserAccountDetails createUserAccountDetails(){
        var user = createUser(UUID.randomUUID());
        return new UserAccountDetails(user, new UserSocialStats(0, 0, 0, 0), 0, 0);
    }

    private User createUser(UUID id) {
        return new User(id, "PaxGPT", "Pax", "Pekpa", "a@gmail.com",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, null, null, true);
    }
}
