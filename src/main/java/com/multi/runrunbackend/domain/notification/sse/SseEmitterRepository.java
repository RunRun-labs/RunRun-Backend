package com.multi.runrunbackend.domain.notification.sse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : SseEmitterRepository
 * @since : 2026-01-05 월요일
 */

@Repository
public class SseEmitterRepository {

  private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

  public SseEmitter get(Long receiverId) {
    return emitters.get(receiverId);
  }

  public void save(Long userId, SseEmitter emitter) {
    SseEmitter prev = emitters.put(userId, emitter);
    if (prev != null) {
      try {
        prev.complete();
      } catch (Exception ignore) {
      }
    }
  }

  public void remove(Long receiverId, SseEmitter emitter) {
    emitters.remove(receiverId);
  }

  public long count() {
    return emitters.size();
  }

}
