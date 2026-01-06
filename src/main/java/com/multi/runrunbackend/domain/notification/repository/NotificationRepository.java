package com.multi.runrunbackend.domain.notification.repository;

import com.multi.runrunbackend.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : NotificationRepository
 * @since : 2026-01-05 월요일
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Slice<Notification> findByReceiver_IdAndIsDeletedFalse(Long receiverId, Pageable pageable);

  long countByReceiver_IdAndIsReadFalse(Long receiverId);

}
