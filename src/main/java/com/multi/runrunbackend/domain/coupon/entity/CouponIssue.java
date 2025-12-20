package com.multi.runrunbackend.domain.coupon.entity;

import com.multi.runrunbackend.common.entitiy.BaseCreatedEntity;
import com.multi.runrunbackend.domain.coupon.constant.CouponIssueStatus;
import com.multi.runrunbackend.domain.coupon.constant.CouponIssueType;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : 사용자에게 실제로 발급된 쿠폰 엔티티. 쿠폰 정책(Coupon)을 기반으로 사용자에게 발급되며, 개별 쿠폰 코드, 만료 시점, 사용 상태를
 * 관리한다. 쿠폰 사용 시 usedAt 이 기록되며, 발급 유형(AUTO / MANUAL)에 따라 발급 경로를 구분한다.
 * @filename : CouponIssue
 * @since : 2025. 12. 17. Wednesday
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "coupon_issue",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "code")
    }
)
public class CouponIssue extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(name = "expiry_at", nullable = false)
    private LocalDateTime expiryAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 30)
    private CouponIssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CouponIssueStatus status;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = CouponIssueStatus.AVAILABLE;
        }
    }
}
