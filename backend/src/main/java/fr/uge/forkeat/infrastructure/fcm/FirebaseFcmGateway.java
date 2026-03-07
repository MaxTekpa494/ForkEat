package fr.uge.forkeat.infrastructure.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import fr.uge.forkeat.service.port.FcmGateway;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Implémentation Firebase réelle de FcmGateway.
 * Active uniquement si app.fcm.enabled=true dans application.properties.
 *
 * Prérequis :
 *   1. Créer un projet Firebase Console
 *   2. Télécharger le fichier service-account JSON
 *   3. Définir FCM_SERVICE_ACCOUNT_PATH et FCM_ENABLED=true dans .env
 */
@Component
@ConditionalOnProperty(name = "app.fcm.enabled", havingValue = "true")
public class FirebaseFcmGateway implements FcmGateway {

    private final Logger logger = LoggerFactory.getLogger(FirebaseFcmGateway.class);

    @Value("${app.fcm.service-account-path}")
    private String serviceAccountPath;

    @PostConstruct
    public void initFirebase() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            try (var serviceAccount = new FileInputStream(serviceAccountPath)) {
                var options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
                logger.info("Firebase initialized from {}", serviceAccountPath);
            }
        }
    }

    @Override
    public void sendToTopic(String topic, String title, String body) {
        var message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setTopic(topic)
                .build();

        FirebaseMessaging.getInstance().sendAsync(message).addListener(
                () -> logger.info("FCM notification sent: topic={}, title={}", topic, title),
                Runnable::run
        );
    }
}
