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
import lombok.RequiredArgsConstructor;
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

    sseEmitterRepository.add(receiverId, emitter);

    emitter.onCompletion(() -> sseEmitterRepository.remove(receiverId, emitter));
    emitter.onTimeout(() -> sseEmitterRepository.remove(receiverId, emitter));
    emitter.onError(e -> sseEmitterRepository.remove(receiverId, emitter));

    try {
      emitter.send(SseEmitter.event()
          .id("init-" + Instant.now().toEpochMilli())
          .name("connected")
          .data("ok"));
    } catch (IOException e) {
      sseEmitterRepository.remove(receiverId, emitter);
    }

    return emitter;
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

  private User getUser(CustomUser principal) {
    return userRepository.findByLoginId(principal.getLoginId())
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
  }


}
