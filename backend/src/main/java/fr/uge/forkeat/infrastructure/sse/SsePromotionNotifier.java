package fr.uge.forkeat.infrastructure.sse;

import fr.uge.forkeat.presentation.dto.superlike.PromotionDTO;
import fr.uge.forkeat.presentation.dto.superlike.PromotionEventDTO;
import fr.uge.forkeat.service.model.superlike.Promotion;
import fr.uge.forkeat.service.port.PromotionSsePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SsePromotionNotifier implements PromotionSsePort {

    private static final long SSE_TIMEOUT = 0L; // pas de timeout côté serveur
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
    private final Logger logger = LoggerFactory.getLogger(SsePromotionNotifier.class);

    public SseEmitter subscribe() {
        var emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(emitter);
        });
        emitter.onError(_ -> emitters.remove(emitter));
        logger.debug("New SSE subscriber. Total: {}", emitters.size());
        return emitter;
    }

    @Override
    public void notifyActivated(Promotion promotion) {
        sendEvent(PromotionEventDTO.activated(PromotionDTO.from(promotion)));
    }

    @Override
    public void notifyExpired(Promotion promotion) {
        sendEvent(PromotionEventDTO.expired(PromotionDTO.from(promotion)));
    }

    private void sendEvent(PromotionEventDTO event) {
        var dead = ConcurrentHashMap.newKeySet();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.type().toLowerCase())
                        .data(event, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                dead.add(emitter);
            }
        });
        emitters.removeAll(dead);
        logger.info("SSE event '{}' sent to {} client(s) ({} dead removed)",
                event.type(), emitters.size(), dead.size());
    }
}
