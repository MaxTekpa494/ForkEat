package fr.uge.forkeat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailService = new MailService(mailSender);
        ReflectionTestUtils.setField(mailService, "mail", "noreply@forkeat.fr");
    }

    @Nested
    class SendTests {

        @Test
        void shouldSendEmailWithCorrectFields() {
            // When
            mailService.send("user@test.com", "Subject", "Body text");

            // Then
            var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());

            var message = captor.getValue();
            assertEquals("noreply@forkeat.fr", message.getFrom());
            assertArrayEquals(new String[]{"user@test.com"}, message.getTo());
            assertEquals("Subject", message.getSubject());
            assertEquals("Body text", message.getText());
        }

        @Test
        void shouldThrowOnNullTo() {
            assertThrows(NullPointerException.class, () -> mailService.send(null, "Subject", "Body"));
        }

        @Test
        void shouldThrowOnNullSubject() {
            assertThrows(NullPointerException.class, () -> mailService.send("to@test.com", null, "Body"));
        }

        @Test
        void shouldThrowOnNullText() {
            assertThrows(NullPointerException.class, () -> mailService.send("to@test.com", "Subject", null));
        }
    }
}
