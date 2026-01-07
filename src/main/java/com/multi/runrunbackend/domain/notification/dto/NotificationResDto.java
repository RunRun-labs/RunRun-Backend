package com.multi.runrunbackend.domain.notification.dto;

import com.multi.runrunbackend.domain.notification.entity.Notification;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : NotificationDto
 * @since : 2026-01-05 월요일
 */

@AllArgsConstructor
@Getter
public class NotificationResDto {

  private Long id;
  private String title;
  private String message;
  private String notificationType;
  private String relatedType;
  private Long relatedId;
  private boolean isRead;
  private LocalDateTime createdAt;

  public static NotificationResDto from(Notification n) {
    return new NotificationResDto(
        n.getId(),
        n.getTitle(),
        n.getMessage(),
        n.getNotificationType().name(),
        n.getRelatedType() == null ? null : n.getRelatedType().name(),
        n.getRelatedId(),
        n.isRead(),
        n.getCreatedAt()
    );
  }

}
