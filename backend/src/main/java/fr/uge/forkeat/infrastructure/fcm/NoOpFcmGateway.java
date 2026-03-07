package fr.uge.forkeat.infrastructure.fcm;

import fr.uge.forkeat.service.port.FcmGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Implémentation no-op de FcmGateway.
 * Active automatiquement quand app.fcm.enabled=false (ou non défini).
 */
@Component
@ConditionalOnMissingBean(FcmGateway.class)
public class NoOpFcmGateway implements FcmGateway {

    private final Logger logger = LoggerFactory.getLogger(NoOpFcmGateway.class);

    @Override
    public void sendToTopic(String topic, String title, String body) {
        logger.debug("FCM disabled — skipping push notification: topic={}, title={}", topic, title);
    }
}
