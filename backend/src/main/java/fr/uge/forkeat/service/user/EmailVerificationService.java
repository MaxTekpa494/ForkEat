package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.external.MailGateway;
import fr.uge.forkeat.service.exception.VerificationException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.VerificationToken;
import fr.uge.forkeat.service.model.user.VerificationTokenType;
import fr.uge.forkeat.service.persistence.UserPersistence;
import fr.uge.forkeat.service.persistence.VerificationTokenPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private final VerificationTokenPersistence tokenPersistence;
    private final UserPersistence userPersistence;
    private final MailGateway mailGateway;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.email.confirmation-expiry-hours}")
    private long EMAIL_CONFIRMATION_EXPIRY_HOURS;

    @Value("${app.email.code-expiry-minutes}")
    private long CODE_EXPIRY_MINUTES;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public EmailVerificationService(VerificationTokenPersistence tokenPersistence,
                                    UserPersistence userPersistence,
                                    MailGateway mailGateway) {
        this.tokenPersistence = tokenPersistence;
        this.userPersistence = userPersistence;
        this.mailGateway = mailGateway;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 15)
    public void sendEmailConfirmation(UUID userId, String email) {
        Objects.requireNonNull(email);
        tokenPersistence.deleteByUserIdAndType(userId, VerificationTokenType.EMAIL_CONFIRMATION);

        var tokenValue = UUID.randomUUID().toString();
        var token = new VerificationToken(
                UUID.randomUUID(), userId, tokenValue,
                VerificationTokenType.EMAIL_CONFIRMATION, null,
                Instant.now().plus(EMAIL_CONFIRMATION_EXPIRY_HOURS, ChronoUnit.HOURS),
                Instant.now()
        );
        tokenPersistence.save(token);

        var link = baseUrl + "/auth/confirm-email?token=" + tokenValue;
        mailGateway.send(email, "ForkEat - Confirmez votre adresse email",
                "Bienvenue sur ForkEat !\n\n"
                        + "Cliquez sur le lien suivant pour confirmer votre email :\n"
                        + link + "\n\n"
                        + "Ce lien expire dans 24 heures.\n\n"
                        + "Si vous n'avez pas créé de compte, ignorez ce message.");
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 10)
    public void confirmEmail(String tokenValue) {
        var token = tokenPersistence.findByToken(tokenValue)
                .orElseThrow(() -> new VerificationException("Lien de confirmation invalide"));

        if (token.isExpired()) {
            tokenPersistence.deleteByUserIdAndType(token.userId(), token.type());
            throw new VerificationException("Le lien de confirmation a expiré. Veuillez en demander un nouveau.");
        }

        if (token.type() != VerificationTokenType.EMAIL_CONFIRMATION) {
            throw new VerificationException("Token invalide");
        }

        userPersistence.updateEmailVerified(token.userId(), true);
        tokenPersistence.deleteByUserIdAndType(token.userId(), VerificationTokenType.EMAIL_CONFIRMATION);
    }

    /**
     * Send a code to reset the password by mail.
     * The password is NOT stored in the token — it will be provided at confirmation time.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 15)
    public void sendPasswordChangeCode(UUID userId, String email) {
        tokenPersistence.deleteByUserIdAndType(userId, VerificationTokenType.PASSWORD_CHANGE);

        var code = generateSixDigitCode();
        var token = new VerificationToken(
                UUID.randomUUID(), userId, code,
                VerificationTokenType.PASSWORD_CHANGE, null,
                Instant.now().plus(CODE_EXPIRY_MINUTES, ChronoUnit.MINUTES),
                Instant.now()
        );
        tokenPersistence.save(token);

        sendChangePasswordCodeMail(email, code);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 10)
    public void confirmPasswordChange(UUID userId, String code, String hashedNewPassword) {
        Objects.requireNonNull(code);
        Objects.requireNonNull(hashedNewPassword);

        var token = tokenPersistence.findByUserIdAndType(userId, VerificationTokenType.PASSWORD_CHANGE)
                .orElseThrow(() -> new VerificationException("Aucun changement de mot de passe en attente"));

        if (token.isExpired()) {
            tokenPersistence.deleteByUserIdAndType(userId, VerificationTokenType.PASSWORD_CHANGE);
            throw new VerificationException("Le code a expiré. Veuillez recommencer.");
        }

        if (!token.token().equals(code)) {
            throw new VerificationException("Code incorrect");
        }

        var user = userPersistence.findById(userId)
                .orElseThrow(() -> new VerificationException("Utilisateur introuvable"));
        userPersistence.saveUser(user, hashedNewPassword);

        tokenPersistence.deleteByUserIdAndType(userId, VerificationTokenType.PASSWORD_CHANGE);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 15)
    public void sendEmailChangeCode(UUID userId, String currentEmail, String newEmail) {
        sendEmailChangeCode(userId, currentEmail, newEmail, false);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 15)
    public void sendEmailChangeCode(UUID userId, String currentEmail, String newEmail, boolean switchingToLocal) {
        if (userPersistence.existsByEmail(newEmail)) {
            throw new VerificationException("Cet email est déjà utilisé");
        }

        tokenPersistence.deleteByUserIdAndType(userId, VerificationTokenType.EMAIL_CHANGE);

        var code = generateSixDigitCode();
        var token = new VerificationToken(
                UUID.randomUUID(), userId, code,
                VerificationTokenType.EMAIL_CHANGE, newEmail,
                Instant.now().plus(CODE_EXPIRY_MINUTES, ChronoUnit.MINUTES),
                Instant.now()
        );
        tokenPersistence.save(token);

        mailGateway.send(currentEmail, "ForkEat - Code de confirmation",
                "Votre code de confirmation pour le changement d'email : " + code + "\n\n"
                        + "Ce code expire dans 10 minutes.\n"
                        + "Si vous n'avez pas demandé ce changement, ignorez ce message.");
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 10)
    public User confirmEmailChange(UUID userId, String code) {
        return confirmEmailChange(userId, code, null);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 10)
    public User confirmEmailChange(UUID userId, String code, String hashedPassword) {
        var validated = validateEmailChangeCode(userId, code);
        var user = validated.user();
        var token = validated.token();

        User result;
        if (hashedPassword != null) {
            var updatedUser = new User(user.id(), user.username(), user.firstName(), user.lastName(),
                    token.newEmail(), user.role(), user.status(), AuthMode.LOCAL,
                    user.createdAt(), Instant.now(), user.emailVerified());
            result = userPersistence.saveUser(updatedUser, hashedPassword);
        } else {
            var updatedUser = new User(user.id(), user.username(), user.firstName(), user.lastName(),
                    token.newEmail(), user.role(), user.status(), user.authMode(),
                    user.createdAt(), Instant.now(), user.emailVerified());
            result = userPersistence.updateUser(updatedUser);
        }

        tokenPersistence.deleteByUserIdAndType(userId, VerificationTokenType.EMAIL_CHANGE);
        return result;
    }

    private record ValidatedEmailChange(User user, VerificationToken token) {}

    private ValidatedEmailChange validateEmailChangeCode(UUID userId, String code) {
        Objects.requireNonNull(code);
        var token = tokenPersistence.findByUserIdAndType(userId, VerificationTokenType.EMAIL_CHANGE)
                .orElseThrow(() -> new VerificationException("Aucun changement d'email en attente"));

        if (token.isExpired()) {
            tokenPersistence.deleteByUserIdAndType(userId, VerificationTokenType.EMAIL_CHANGE);
            throw new VerificationException("Le code a expiré. Veuillez recommencer.");
        }

        if (!token.token().equals(code)) {
            throw new VerificationException("Code incorrect");
        }

        var user = userPersistence.findById(userId)
                .orElseThrow(() -> new VerificationException("Utilisateur introuvable"));

        return new ValidatedEmailChange(user, token);
    }

    private void sendChangePasswordCodeMail(String email, String code) {
        mailGateway.send(email, "ForkEat - Code de confirmation",
                "Votre code de confirmation pour le changement de mot de passe : " + code + "\n\n"
                        + "Ce code expire dans 10 minutes.\n"
                        + "Si vous n'avez pas demandé ce changement, ignorez ce message.");
    }

    private String generateSixDigitCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }
}