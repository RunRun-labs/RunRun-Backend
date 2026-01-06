package com.multi.runrunbackend.domain.notification.sse;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : NotificationPushService
 * @since : 2026-01-05 월요일
 */

@Service
@RequiredArgsConstructor
public class NotificationPushService {

  private final SseEmitterRepository repo;

  public void sendToUser(Long receiverId, Object payload) {
    List<SseEmitter> list = repo.get(receiverId);
    if (list.isEmpty()) {
      return;
    }

    String eventId = "evt-" + Instant.now().toEpochMilli();

    for (SseEmitter emitter : list) {
      try {
        emitter.send(SseEmitter.event()
            .id(eventId)
            .name("notification")
            .data(payload));
      } catch (IOException e) {
        repo.remove(receiverId, emitter);
      }
    }
  }

}
