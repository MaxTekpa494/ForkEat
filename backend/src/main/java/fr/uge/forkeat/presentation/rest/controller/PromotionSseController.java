package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.infrastructure.sse.SsePromotionNotifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/promotions")
public class PromotionSseController {

    private final SsePromotionNotifier sseNotifier;

    public PromotionSseController(SsePromotionNotifier sseNotifier) {
        this.sseNotifier = sseNotifier;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseNotifier.subscribe();
    }
}
