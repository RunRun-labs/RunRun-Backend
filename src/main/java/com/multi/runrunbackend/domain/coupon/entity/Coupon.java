package com.multi.runrunbackend.domain.coupon.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import com.multi.runrunbackend.domain.coupon.constant.CouponBenefitType;
import com.multi.runrunbackend.domain.coupon.constant.CouponChannel;
import com.multi.runrunbackend.domain.coupon.constant.CouponCodeType;
import com.multi.runrunbackend.domain.coupon.constant.CouponStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : 쿠폰 정책 정보를 관리하는 엔티티. 쿠폰의 기본 정보(이름, 혜택, 기간, 발급 채널, 코드 타입 등)를 정의하며 실제 사용자에게 발급되는 쿠폰
 * 인스턴스(CouponIssue)의 기준이 된다. 발급 수량 제한, 자동 발급 여부, 쿠폰 상태 관리에 사용된다.
 * @filename : Coupon
 * @since : 2025. 12. 17. Wednesday
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "coupon")
public class Coupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * null 이면 무제한
     *
     */
    private Integer quantity;

    @Column(nullable = false)
    private Integer issuedCount;

    @Column(nullable = false, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "code_type", nullable = false, length = 30)
    private CouponCodeType codeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CouponChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "benefit_type", nullable = false, length = 30)
    private CouponBenefitType benefitType;

    @Column(nullable = false)
    private Integer benefitValue;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "auto_issue", nullable = false)
    private Boolean autoIssue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CouponStatus status;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = CouponStatus.DRAFT;
        }

        if (this.issuedCount == null) {
            this.issuedCount = 0;
        }
    }

}
