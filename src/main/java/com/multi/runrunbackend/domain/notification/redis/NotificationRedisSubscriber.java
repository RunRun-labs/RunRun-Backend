package com.multi.runrunbackend.domain.notification.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.domain.notification.dto.NotificationResDto;
import com.multi.runrunbackend.domain.notification.event.NotificationSignal;
import com.multi.runrunbackend.domain.notification.repository.NotificationRepository;
import com.multi.runrunbackend.domain.notification.sse.NotificationPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;


/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : NotificationRedisSubscriber
 * @since : 2026-01-05 월요일
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationRedisSubscriber implements MessageListener {

  private final ObjectMapper objectMapper;
  private final NotificationRepository repo;
  private final NotificationPushService pushService;

  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      NotificationSignal signal =
          objectMapper.readValue(message.getBody(), NotificationSignal.class);

      repo.findById(signal.notificationId()).ifPresent(n -> {
        pushService.sendToUser(signal.receiverId(), NotificationResDto.from(n));
      });

    } catch (Exception e) {
      log.debug("[Notification Sub RECEIVED] bytes={}, pattern={}",
          message.getBody() != null ? message.getBody().length : -1,
          pattern != null ? new String(pattern) : null
      );
    }
  }
}
