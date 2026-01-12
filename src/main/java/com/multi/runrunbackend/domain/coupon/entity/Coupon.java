package com.multi.runrunbackend.domain.coupon.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import com.multi.runrunbackend.domain.coupon.constant.CouponBenefitType;
import com.multi.runrunbackend.domain.coupon.constant.CouponChannel;
import com.multi.runrunbackend.domain.coupon.constant.CouponCodeType;
import com.multi.runrunbackend.domain.coupon.constant.CouponStatus;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponCreateReqDto;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponUpdateReqDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.SQLRestriction;

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
@Check(constraints = """
    (
      (benefit_type = 'FIXED_DISCOUNT' AND benefit_value BETWEEN 1000 AND 9900 AND MOD(benefit_value, 1000) = 0)
      OR
      (benefit_type = 'RATE_DISCOUNT' AND benefit_value BETWEEN 1 AND 100)
    )
    """)
@Table(
    name = "coupon",
    uniqueConstraints = @UniqueConstraint(name = "uk_coupon_code", columnNames = "code")
)
@SQLRestriction("status <> 'DELETED'")
public class Coupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;


    private Integer quantity;

    @Column(nullable = false)
    private Integer issuedCount;

    @Column(nullable = false, length = 100, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "code_type", nullable = false)
    private CouponCodeType codeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "benefit_type", nullable = false)
    private CouponBenefitType benefitType;

    @Column(nullable = false)
    private Integer benefitValue;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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

    public static Coupon create(CouponCreateReqDto req, String code) {
        Coupon coupon = new Coupon();

        coupon.name = req.getName();
        coupon.description = req.getDescription();
        coupon.quantity = req.getQuantity();
        coupon.codeType = req.getCodeType();
        coupon.channel = req.getChannel();
        coupon.benefitType = req.getBenefitType();
        coupon.benefitValue = req.getBenefitValue();
        coupon.startAt = req.getStartAt();
        coupon.endAt = req.getEndAt();
        coupon.code = code;

        return coupon;

    }


    public void update(CouponUpdateReqDto req) {
        this.name = req.getName();
        this.description = req.getDescription();
        this.quantity = req.getQuantity();
        this.codeType = req.getCodeType();
        this.channel = req.getChannel();
        this.benefitType = req.getBenefitType();
        this.benefitValue = req.getBenefitValue();
        this.startAt = req.getStartAt();
        this.endAt = req.getEndAt();
    }

    public void delete() {
        if (this.status == CouponStatus.DELETED) {
            return;
        }
        this.status = CouponStatus.DELETED;
    }

    public void soldOut() {
        if (this.status != CouponStatus.ACTIVE) {
            return;
        }
        this.status = CouponStatus.SOLD_OUT;

    }
}
