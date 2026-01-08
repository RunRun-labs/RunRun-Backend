package com.multi.runrunbackend.domain.notification.sse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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

  private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

  public void add(Long receiverId, SseEmitter emitter) {
    emitters.computeIfAbsent(receiverId, k -> new CopyOnWriteArrayList<>()).add(emitter);
  }

  public List<SseEmitter> get(Long receiverId) {
    return emitters.getOrDefault(receiverId, List.of());
  }

  public void remove(Long receiverId, SseEmitter emitter) {
    List<SseEmitter> list = emitters.get(receiverId);
    if (list != null) {
      list.remove(emitter);
      if (list.isEmpty()) {
        emitters.remove(receiverId);
      }
    }
  }


}
