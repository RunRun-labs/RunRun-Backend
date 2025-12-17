package com.multi.runrunbackend.domain.notification.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.notification.constant.NotificationType;
import com.multi.runrunbackend.domain.notification.constant.RelatedType;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * @author : KIMGWANGHO
 * @description : 알림 관리 엔티티
 * @filename : Notification
 * @since : 2025-12-17 수요일
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "notification")
@SQLRestriction("is_deleted = false")
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User receiver;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(length = 255, nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 20, nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_type", length = 50)
    private RelatedType relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;


}