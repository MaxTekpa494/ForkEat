package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.MailService;
import fr.uge.forkeat.service.exception.VerificationException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.model.user.VerificationToken;
import fr.uge.forkeat.service.model.user.VerificationTokenType;
import fr.uge.forkeat.service.persistence.UserPersistence;
import fr.uge.forkeat.service.persistence.VerificationTokenPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private VerificationTokenPersistence tokenPersistence;
    @Mock
    private UserPersistence userPersistence;
    @Mock
    private MailService mailService;

    @InjectMocks
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "EMAIL_CONFIRMATION_EXPIRY_HOURS", 24L);
        ReflectionTestUtils.setField(service, "CODE_EXPIRY_MINUTES", 10L);
    }

    private User createUser(UUID id) {
        return new User(id, "testuser", "Test", "User", "test@forkeat.fr",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL,
                Instant.now(), Instant.now(), false);
    }

    private VerificationToken createToken(UUID userId, VerificationTokenType type, String tokenValue,
                                           String newEmail, String passwordHash, Instant expiresAt) {
        return new VerificationToken(UUID.randomUUID(), userId, tokenValue, type, newEmail, passwordHash, expiresAt, Instant.now());
    }

    // ========== SEND EMAIL CONFIRMATION ==========

    @Nested
    class SendEmailConfirmationTests {

        @Test
        void shouldDeleteOldTokenAndSaveNewOne() {
            var userId = UUID.randomUUID();
            when(tokenPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.sendEmailConfirmation(userId, "test@forkeat.fr");

            verify(tokenPersistence).deleteByUserIdAndType(userId, VerificationTokenType.EMAIL_CONFIRMATION);
            verify(tokenPersistence).save(any(VerificationToken.class));
        }

        @Test
        void shouldSendEmailWithConfirmationLink() {
            var userId = UUID.randomUUID();
            when(tokenPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.sendEmailConfirmation(userId, "test@forkeat.fr");

            var captor = ArgumentCaptor.forClass(String.class);
            verify(mailService).send(eq("test@forkeat.fr"), any(), captor.capture());
            assertTrue(captor.getValue().contains("http://localhost:8080/auth/confirm-email?token="));
        }

        @Test
        void shouldSaveTokenWithCorrectType() {
            var userId = UUID.randomUUID();
            when(tokenPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.sendEmailConfirmation(userId, "test@forkeat.fr");

            var captor = ArgumentCaptor.forClass(VerificationToken.class);
            verify(tokenPersistence).save(captor.capture());
            assertEquals(VerificationTokenType.EMAIL_CONFIRMATION, captor.getValue().type());
            assertEquals(userId, captor.getValue().userId());
        }
    }

    // ========== CONFIRM EMAIL ==========

    @Nested
    class ConfirmEmailTests {

        @Test
        void shouldConfirmEmailWithValidToken() {
            var userId = UUID.randomUUID();
            var token = createToken(userId, VerificationTokenType.EMAIL_CONFIRMATION, "valid-token",
                    null, null, Instant.now().plus(1, ChronoUnit.HOURS));

            when(tokenPersistence.findByToken("valid-token")).thenReturn(Optional.of(token));

            service.confirmEmail("valid-token");

            verify(userPersistence).updateEmailVerified(userId, true);
            verify(tokenPersistence).deleteByUserIdAndType(userId, VerificationTokenType.EMAIL_CONFIRMATION);
        }

        @Test
        void shouldThrowWhenTokenNotFound() {
            when(tokenPersistence.findByToken("unknown")).thenReturn(Optional.empty());

            assertThrows(VerificationException.class, () -> service.confirmEmail("unknown"));
        }

        @Test
        void shouldThrowWhenTokenExpired() {
            var userId = UUID.randomUUID();
            var token = createToken(userId, VerificationTokenType.EMAIL_CONFIRMATION, "expired",
                    null, null, Instant.now().minus(1, ChronoUnit.HOURS));

            when(tokenPersistence.findByToken("expired")).thenReturn(Optional.of(token));

            assertThrows(VerificationException.class, () -> service.confirmEmail("expired"));
            verify(tokenPersistence).deleteByUserIdAndType(userId, VerificationTokenType.EMAIL_CONFIRMATION);
            verify(userPersistence, never()).updateEmailVerified(any(), anyBoolean());
        }

        @Test
        void shouldThrowWhenWrongTokenType() {
            var userId = UUID.randomUUID();
            var token = createToken(userId, VerificationTokenType.PASSWORD_CHANGE, "wrong-type",
                    null, null, Instant.now().plus(1, ChronoUnit.HOURS));

            when(tokenPersistence.findByToken("wrong-type")).thenReturn(Optional.of(token));

            assertThrows(VerificationException.class, () -> service.confirmEmail("wrong-type"));
        }
    }

    // ========== SEND PASSWORD CHANGE CODE ==========

    @Nested
    class SendPasswordChangeCodeTests {

        @Test
        void shouldDeleteOldTokenAndSaveNew() {
            var userId = UUID.randomUUID();
            when(tokenPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.sendPasswordChangeCode(userId, "test@forkeat.fr", "hashed-new-pw");

            verify(tokenPersistence).deleteByUserIdAndType(userId, VerificationTokenType.PASSWORD_CHANGE);

            var captor = ArgumentCaptor.forClass(VerificationToken.class);
            verify(tokenPersistence).save(captor.capture());
            assertEquals(VerificationTokenType.PASSWORD_CHANGE, captor.getValue().type());
            assertEquals("hashed-new-pw", captor.getValue().passwordHash());
        }

        @Test
        void shouldSendEmailWithCode() {
            var userId = UUID.randomUUID();
            when(tokenPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.sendPasswordChangeCode(userId, "test@forkeat.fr", "hashed-pw");

            verify(mailService).send(eq("test@forkeat.fr"), any(), any());
        }
    }

    // ========== CONFIRM PASSWORD CHANGE ==========

    @Nested
    class ConfirmPasswordChangeTests {

        @Test
        void shouldConfirmWithValidCode() {
            var userId = UUID.randomUUID();
            var user = createUser(userId);
            var token = createToken(userId, VerificationTokenType.PASSWORD_CHANGE, "123456",
                    null, "new-hashed-pw", Instant.now().plus(10, ChronoUnit.MINUTES));

            when(tokenPersistence.findByUserIdAndType(userId, VerificationTokenType.PASSWORD_CHANGE))
                    .thenReturn(Optional.of(token));
            when(userPersistence.findById(userId)).thenReturn(Optional.of(user));
            when(userPersistence.saveUser(any(), eq("new-hashed-pw"))).thenReturn(user);

            service.confirmPasswordChange(userId, "123456");

            verify(userPersistence).saveUser(any(), eq("new-hashed-pw"));
            verify(tokenPersistence).deleteByUserIdAndType(userId, VerificationTokenType.PASSWORD_CHANGE);
        }

        @Test
        void shouldThrowWhenNoPendingChange() {
            var userId = UUID.randomUUID();
            when(tokenPersistence.findByUserIdAndType(userId, VerificationTokenType.PASSWORD_CHANGE))
                    .thenReturn(Optional.empty());

            assertThrows(VerificationException.class, () -> service.confirmPasswordChange(userId, "123456"));
        }

        @Test
        void shouldThrowWhenCodeExpired() {
            var userId = UUID.randomUUID();
            var token = createToken(userId, VerificationTokenType.PASSWORD_CHANGE, "123456",
                    null, "pw", Instant.now().minus(1, ChronoUnit.HOURS));

            when(tokenPersistence.findByUserIdAndType(userId, VerificationTokenType.PASSWORD_CHANGE))
                    .thenReturn(Optional.of(token));

            assertThrows(VerificationException.class, () -> service.confirmPasswordChange(userId, "123456"));
            verify(tokenPersistence).deleteByUserIdAndType(userId, VerificationTokenType.PASSWORD_CHANGE);
        }

        @Test
        void shouldThrowWhenWrongCode() {
            var userId = UUID.randomUUID();
            var token = createToken(userId, VerificationTokenType.PASSWORD_CHANGE, "123456",
                    null, "pw", Instant.now().plus(10, ChronoUnit.MINUTES));

            when(tokenPersistence.findByUserIdAndType(userId, VerificationTokenType.PASSWORD_CHANGE))
                    .thenReturn(Optional.of(token));

            assertThrows(VerificationException.class, () -> service.confirmPasswordChange(userId, "000000"));
        }
    }

    // ========== SEND EMAIL CHANGE CODE ==========

    @Nested
    class SendEmailChangeCodeTests {

        @Test
        void shouldThrowWhenEmailAlreadyUsed() {
            var userId = UUID.randomUUID();
            when(userPersistence.existsByEmail("taken@forkeat.fr")).thenReturn(true);

            assertThrows(VerificationException.class,
                    () -> service.sendEmailChangeCode(userId, "old@forkeat.fr", "taken@forkeat.fr"));
            verifyNoInteractions(tokenPersistence);
        }

        @Test
        void shouldSaveTokenWithNewEmailAsPayload() {
            var userId = UUID.randomUUID();
            when(userPersistence.existsByEmail("new@forkeat.fr")).thenReturn(false);
            when(tokenPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.sendEmailChangeCode(userId, "old@forkeat.fr", "new@forkeat.fr");

            var captor = ArgumentCaptor.forClass(VerificationToken.class);
            verify(tokenPersistence).save(captor.capture());
            assertEquals(VerificationTokenType.EMAIL_CHANGE, captor.getValue().type());
            assertEquals("new@forkeat.fr", captor.getValue().newEmail());
        }

        @Test
        void shouldSendEmailToCurrentAddress() {
            var userId = UUID.randomUUID();
            when(userPersistence.existsByEmail("new@forkeat.fr")).thenReturn(false);
            when(tokenPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.sendEmailChangeCode(userId, "old@forkeat.fr", "new@forkeat.fr");

            verify(mailService).send(eq("old@forkeat.fr"), any(), any());
        }
    }

    // ========== CONFIRM EMAIL CHANGE ==========

    @Nested
    class ConfirmEmailChangeTests {

        @Test
        void shouldUpdateEmailWithValidCode() {
            var userId = UUID.randomUUID();
            var user = createUser(userId);
            var token = createToken(userId, VerificationTokenType.EMAIL_CHANGE, "654321",
                    "new@forkeat.fr", null, Instant.now().plus(10, ChronoUnit.MINUTES));

            when(tokenPersistence.findByUserIdAndType(userId, VerificationTokenType.EMAIL_CHANGE))
                    .thenReturn(Optional.of(token));
            when(userPersistence.findById(userId)).thenReturn(Optional.of(user));
            when(userPersistence.updateUser(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.confirmEmailChange(userId, "654321");

            assertNotNull(result);
            verify(userPersistence).updateUser(any());
            verify(tokenPersistence).deleteByUserIdAndType(userId, VerificationTokenType.EMAIL_CHANGE);
        }

        @Test
        void shouldThrowWhenNoPendingChange() {
            var userId = UUID.randomUUID();
            when(tokenPersistence.findByUserIdAndType(userId, VerificationTokenType.EMAIL_CHANGE))
                    .thenReturn(Optional.empty());

            assertThrows(VerificationException.class, () -> service.confirmEmailChange(userId, "123456"));
        }

        @Test
        void shouldThrowWhenExpired() {
            var userId = UUID.randomUUID();
            var token = createToken(userId, VerificationTokenType.EMAIL_CHANGE, "654321",
                    "new@forkeat.fr", null, Instant.now().minus(1, ChronoUnit.HOURS));

            when(tokenPersistence.findByUserIdAndType(userId, VerificationTokenType.EMAIL_CHANGE))
                    .thenReturn(Optional.of(token));

            assertThrows(VerificationException.class, () -> service.confirmEmailChange(userId, "654321"));
        }

        @Test
        void shouldThrowWhenWrongCode() {
            var userId = UUID.randomUUID();
            var token = createToken(userId, VerificationTokenType.EMAIL_CHANGE, "654321",
                    "new@forkeat.fr", null, Instant.now().plus(10, ChronoUnit.MINUTES));

            when(tokenPersistence.findByUserIdAndType(userId, VerificationTokenType.EMAIL_CHANGE))
                    .thenReturn(Optional.of(token));

            assertThrows(VerificationException.class, () -> service.confirmEmailChange(userId, "000000"));
        }
    }

    // ========== CONFIRM EMAIL CHANGE WITH PASSWORD (OAuth flow) ==========

    @Nested
    class ConfirmEmailChangeWithPasswordTests {

        @Test
        void shouldUpdateEmailAndAuthModeWhenTokenHasPasswordHash() {
            var userId = UUID.randomUUID();
            var user = createUser(userId);
            var token = createToken(userId, VerificationTokenType.EMAIL_CHANGE, "654321",
                    "new@forkeat.fr", "hashed-pw", Instant.now().plus(10, ChronoUnit.MINUTES));

            when(tokenPersistence.findByUserIdAndType(userId, VerificationTokenType.EMAIL_CHANGE))
                    .thenReturn(Optional.of(token));
            when(userPersistence.findById(userId)).thenReturn(Optional.of(user));
            when(userPersistence.saveUser(any(), eq("hashed-pw"))).thenAnswer(inv -> inv.getArgument(0));

            var result = service.confirmEmailChange(userId, "654321");

            assertNotNull(result);

            var captor = ArgumentCaptor.forClass(User.class);
            verify(userPersistence).saveUser(captor.capture(), eq("hashed-pw"));
            assertEquals(AuthMode.LOCAL, captor.getValue().authMode());
        }
    }
}
