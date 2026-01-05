package com.multi.runrunbackend.domain.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : NotificationAfterCommitListener
 * @since : 2026-01-05 월요일
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationAfterCommitListener {

  private static final String CHANNEL = "notifications";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onCreated(NotificationCreatedEvent event) {
    try {
      String json = objectMapper.writeValueAsString(
          new NotificationSignal(event.notificationId(), event.receiverId())
      );
      redisTemplate.convertAndSend(CHANNEL, json);
    } catch (Exception e) {
      log.error(
          "[Notification Pub FAILED] channel={}, notificationId={}, receiverId={}",
          CHANNEL,
          event.notificationId(),
          event.receiverId(),
          e
      );
    }
  }
}
