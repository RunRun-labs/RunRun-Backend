package com.multi.runrunbackend.domain.notification.sse;

import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : NotificationPushService
 * @since : 2026-01-05 월요일
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPushService {

  private final SseEmitterRepository repo;

  public void sendToUser(Long receiverId, Object payload) {
    SseEmitter emitter = repo.get(receiverId);
    if (emitter == null) {
      log.warn("[Notification Push FAILED] receiverId={}, emitter=null (not subscribed)",
          receiverId);
      return;
    }

    String eventId = "evt-" + Instant.now().toEpochMilli();

    try {
      emitter.send(SseEmitter.event()
          .id(eventId)
          .name("notification")
          .data(payload));
      log.info("[Notification Push SUCCESS] receiverId={}, eventId={}", receiverId, eventId);
    } catch (IOException e) {
      log.debug("[Notification Push FAILED] receiverId={}, error={}, removing emitter",
          receiverId, e.getClass().getSimpleName() + ": " + e.getMessage());
      repo.remove(receiverId, emitter);
    } catch (RuntimeException e) {
      // IllegalStateException, AsyncRequestNotUsableException 등 모든 RuntimeException 처리
      log.debug("[Notification Push FAILED] receiverId={}, error={}, removing emitter",
          receiverId, e.getClass().getSimpleName() + ": " + e.getMessage());
      repo.remove(receiverId, emitter);
    }
  }
}
