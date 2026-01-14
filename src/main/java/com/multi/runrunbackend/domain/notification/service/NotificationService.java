package com.multi.runrunbackend.domain.notification.service;

import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.notification.constant.NotificationType;
import com.multi.runrunbackend.domain.notification.constant.RelatedType;
import com.multi.runrunbackend.domain.notification.dto.NotificationResDto;
import com.multi.runrunbackend.domain.notification.entity.Notification;
import com.multi.runrunbackend.domain.notification.event.NotificationCreatedEvent;
import com.multi.runrunbackend.domain.notification.repository.NotificationRepository;
import com.multi.runrunbackend.domain.notification.sse.SseEmitterRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : NotificationService
 * @since : 2026-01-05 월요일
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final ApplicationEventPublisher publisher;
  private final SseEmitterRepository sseEmitterRepository;
  private final UserRepository userRepository;


  @Transactional
  public Long create(
      User receiver,
      String title,
      String message,
      NotificationType notificationType,
      RelatedType relatedType,
      Long relatedId
  ) {
    Notification saved = notificationRepository.save(Notification.builder()
        .receiver(receiver)
        .title(title)
        .message(message)
        .notificationType(notificationType)
        .relatedType(relatedType)
        .relatedId(relatedId)
        .isRead(false)
        .build());

    publisher.publishEvent(new NotificationCreatedEvent(saved.getId(), receiver.getId()));

    return saved.getId();
  }

  public SseEmitter subscribe(CustomUser principal) {

    Long receiverId = getUser(principal).getId();

    long timeoutMs = 60L * 60L * 1000L; // 1시간
    SseEmitter emitter = new SseEmitter(timeoutMs);

    sseEmitterRepository.save(receiverId, emitter);

    emitter.onCompletion(() -> sseEmitterRepository.remove(receiverId, emitter));
    emitter.onTimeout(() -> sseEmitterRepository.remove(receiverId, emitter));
    emitter.onError(e -> sseEmitterRepository.remove(receiverId, emitter));

    try {
      emitter.send(SseEmitter.event()
          .id("init-" + Instant.now().toEpochMilli())
          .name("connected")
          .data("ok"));
    } catch (IOException e) {
      log.debug("[SSE Init FAILED] receiverId={}, error={}", receiverId, e.getClass().getSimpleName());
      sseEmitterRepository.remove(receiverId, emitter);
    } catch (RuntimeException e) {
      // IllegalStateException, AsyncRequestNotUsableException 등 모든 RuntimeException 처리
      log.debug("[SSE Init FAILED] receiverId={}, error={}", receiverId, e.getClass().getSimpleName());
      sseEmitterRepository.remove(receiverId, emitter);
    }

    // Heartbeat 주기 전송 (20초마다)
    scheduleHeartbeat(receiverId, emitter);

    log.info("[SSE] subscribe userId={}, emitterHash={}",
        receiverId, emitter.hashCode());
    return emitter;
  }

  /**
   * SSE 연결에 대해 주기적으로 heartbeat를 전송하여 연결을 유지합니다. 프록시나 브라우저가 연결을 끊지 않도록 합니다.
   */
  private void scheduleHeartbeat(Long receiverId, SseEmitter emitter) {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    scheduler.scheduleAtFixedRate(() -> {
      try {
        // emitter가 여전히 유효한지 확인
        SseEmitter currentEmitter = sseEmitterRepository.get(receiverId);
        if (currentEmitter == null || currentEmitter != emitter) {
          log.debug("[SSE Heartbeat] receiverId={}, emitter changed or removed, stopping heartbeat",
              receiverId);
          scheduler.shutdown();
          return;
        }

        emitter.send(SseEmitter.event()
            .id("heartbeat-" + Instant.now().toEpochMilli())
            .name("heartbeat")
            .data("ping"));

        log.debug("[SSE Heartbeat] receiverId={}", receiverId);
      } catch (IOException e) {
        log.debug("[SSE Heartbeat FAILED] receiverId={}, removing emitter, error={}",
            receiverId, e.getClass().getSimpleName());
        sseEmitterRepository.remove(receiverId, emitter);
        scheduler.shutdown();
      } catch (RuntimeException e) {
        // IllegalStateException, AsyncRequestNotUsableException 등 모든 RuntimeException 처리
        log.debug("[SSE Heartbeat FAILED] receiverId={}, removing emitter, error={}",
            receiverId, e.getClass().getSimpleName());
        sseEmitterRepository.remove(receiverId, emitter);
        scheduler.shutdown();
      }
    }, 20, 20, TimeUnit.SECONDS); // 20초마다 전송
  }


  @Transactional(readOnly = true)
  public Slice<NotificationResDto> getRemainingNotifications(CustomUser principal,
      Pageable pageable) {

    Long receiverId = getUser(principal).getId();

    return notificationRepository.findByReceiver_IdAndIsDeletedFalse(receiverId, pageable)
        .map(NotificationResDto::from);
  }

  @Transactional
  public void markAsRead(Long notificationId, CustomUser principal) {
    User user = getUser(principal);

    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND));

    if (!notification.getReceiver().getId().equals(user.getId())) {
      throw new ForbiddenException(ErrorCode.READ_DENIED);
    }

    notification.markAsRead();
  }

  @Transactional(readOnly = true)
  public long getUnreadCount(CustomUser principal) {
    Long receiverId = getUser(principal).getId();
    log.info("조회 요청 유저: {}, 내부 ID: {}", principal.getLoginId(), receiverId); // [추가]
    return notificationRepository.countByReceiver_IdAndIsReadFalse(receiverId);
  }

  private User getUser(CustomUser principal) {
    return userRepository.findByLoginId(principal.getLoginId())
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
  }


}
