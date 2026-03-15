package fr.uge.forkeat.service.port;

/**
 * Port hexagonal pour l'envoi de notifications push Firebase (FCM).
 * Deux implémentations :
 *   - FirebaseFcmGateway  : réelle (activée par app.fcm.enabled=true)
 *   - NoOpFcmGateway      : no-op en dev / tests
 */
public interface FcmGateway {
    void sendToTopic(String topic, String title, String body);
}
