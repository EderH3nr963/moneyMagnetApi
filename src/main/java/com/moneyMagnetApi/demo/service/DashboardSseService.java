package com.moneyMagnetApi.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DashboardSseService {

    public static final String CONNECTED_EVENT = "CONNECTED";
    
    private static final long TIMEOUT = 30 * 60 * 1000L;
    
    private final Map<UUID, Set<SseEmitter>> emittersByUser =
            new ConcurrentHashMap<>();
    
    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        subscribe(userId, emitter);
        return emitter;
    }

    void subscribe(UUID userId, SseEmitter emitter) {
        
        emittersByUser
                .computeIfAbsent(
                        userId,
                        ignored -> ConcurrentHashMap.newKeySet()
                )
                .add(emitter);
        
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        
        emitter.onTimeout(() -> {
            emitter.complete();
            removeEmitter(userId, emitter);
        });
        
        emitter.onError(error -> removeEmitter(userId, emitter));

        try {
            send(emitter, CONNECTED_EVENT, Map.of("connected", true));
        } catch (IOException | IllegalStateException exception) {
            removeEmitter(userId, emitter);
            emitter.completeWithError(exception);
        }
        
    }
    
    public void emitToUser(
            UUID userId,
            String eventName,
            Object data
    ) {
        Set<SseEmitter> userEmitters = emittersByUser.get(userId);
        
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }
        
        for (SseEmitter emitter : userEmitters) {
            try {
                send(emitter, eventName, data);
            } catch (IOException | IllegalStateException exception) {
                removeEmitter(userId, emitter);
                emitter.completeWithError(exception);
            }
        }
    }

    private void send(SseEmitter emitter, String eventName, Object data) throws IOException {
        synchronized (emitter) {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .id(UUID.randomUUID().toString())
                    .name(eventName);
            if (data != null) {
                event.data(data);
            }
            emitter.send(event);
        }
    }
    
    private void removeEmitter(
            UUID userId,
            SseEmitter emitter
    ) {
        Set<SseEmitter> userEmitters = emittersByUser.get(userId);
        
        if (userEmitters == null) {
            return;
        }
        
        userEmitters.remove(emitter);
        
        if (userEmitters.isEmpty()) {
            emittersByUser.remove(userId, userEmitters);
        }
    }
}
