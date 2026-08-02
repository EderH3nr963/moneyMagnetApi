package com.moneyMagnetApi.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardSseServiceTest {

    @Test
    void shouldImmediatelySendConnectedEventAndThenUserEvent() throws Exception {
        DashboardSseService service = new DashboardSseService();
        UUID userId = UUID.randomUUID();

        RecordingEmitter emitter = new RecordingEmitter();
        service.subscribe(userId, emitter);
        service.emitToUser(userId, "ITEM_CREATED_UPDATED", null);

        assertThat(emitter.events).hasSize(2);
    }

    private static class RecordingEmitter extends SseEmitter {
        private final List<SseEventBuilder> events = new ArrayList<>();

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            events.add(builder);
        }
    }
}
