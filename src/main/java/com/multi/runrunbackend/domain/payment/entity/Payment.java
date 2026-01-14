package com.multi.runrunbackend.domain.payment.entity;

import com.multi.runrunbackend.common.entitiy.BaseCreatedEntity;
import com.multi.runrunbackend.domain.coupon.entity.CouponIssue;
import com.multi.runrunbackend.domain.payment.constant.MembershipGrade;
import com.multi.runrunbackend.domain.payment.constant.PaymentMethod;
import com.multi.runrunbackend.domain.payment.constant.PaymentStatus;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : BoKyung
 * @description : 결제 내역 엔티티
 * @filename : Payment
 * @since : 25. 12. 17. 수요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_grade", nullable = false, length = 20)
    private MembershipGrade membershipGrade;

    @Column(name = "original_amount", nullable = false)
    private Integer originalAmount;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;

    @Column(name = "final_amount")
    private Integer finalAmount;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_issue_id")
    private CouponIssue couponIssue;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_key", length = 100)
    private String paymentKey;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "billing_key", length = 200)
    private String billingKey;

    private Boolean isAutoPayment = false;

    @Column(name = "card_number", length = 20)
    private String cardNumber;

    private LocalDateTime approvedAt;

    private LocalDateTime canceledAt;


    /**
     * @description : 엔티티 생성 전 기본값 설정
     */
    @PrePersist
    public void prePersist() {
        if (this.membershipGrade == null) {
            this.membershipGrade = MembershipGrade.PREMIUM;
        }
        if (this.paymentStatus == null) {
            this.paymentStatus = PaymentStatus.READY;
        }
        if (this.discountAmount == null) {
            this.discountAmount = 0;
        }
        if (this.isAutoPayment == null) {
            this.isAutoPayment = false;
        }
    }

    /**
     * @description : 결제 생성 정적 팩토리 메서드
     */
    public static Payment create(
            User user,
            Integer originalAmount,
            Integer discountAmount,
            String orderId,
            CouponIssue couponIssue
    ) {
        Payment payment = new Payment();
        payment.user = user;
        payment.membershipGrade = MembershipGrade.PREMIUM;
        payment.originalAmount = originalAmount;
        payment.discountAmount = discountAmount;
        payment.finalAmount = originalAmount - discountAmount;
        payment.paymentStatus = PaymentStatus.READY;
        payment.orderId = orderId;
        payment.couponIssue = couponIssue;

        if (couponIssue != null) {
            payment.couponCode = couponIssue.getCoupon().getCode();
        }

        payment.isAutoPayment = false;
        return payment;
    }

    /**
     * @description : 자동결제용 Payment 생성
     */
    public static Payment createForAutoPayment(
            User user,
            Integer originalAmount,
            Integer discountAmount,
            String orderId,
            String billingKey
    ) {
        Payment payment = new Payment();
        payment.user = user;
        payment.membershipGrade = MembershipGrade.PREMIUM;
        payment.originalAmount = originalAmount;
        payment.discountAmount = discountAmount;
        payment.finalAmount = originalAmount - discountAmount;
        payment.paymentStatus = PaymentStatus.READY;
        payment.orderId = orderId;
        payment.billingKey = billingKey;
        payment.couponIssue = null;
        payment.isAutoPayment = true;
        return payment;
    }

    /**
     * @description : 결제 완료 처리
     */
    public void complete(String paymentKey, PaymentMethod paymentMethod, String billingKey, String cardNumber) {
        this.paymentStatus = PaymentStatus.DONE;
        this.paymentKey = paymentKey;
        this.paymentMethod = paymentMethod;
        this.approvedAt = LocalDateTime.now();

        // 빌링키가 있으면 저장
        if (billingKey != null && !billingKey.isBlank()) {
            this.billingKey = billingKey;
        }

        // 카드번호가 있으면 저장
        if (cardNumber != null && !cardNumber.isBlank()) {
            this.cardNumber = cardNumber;
        }
    }

    /**
     * @description : 결제 실패 처리
     */
    public void fail() {
        this.paymentStatus = PaymentStatus.FAILED;
    }

    /**
     * @description : 시스템 자동 취소 처리 (로그에만 이유 기록할 예정)
     */
    public void cancelBySystem(String reason) {
        this.paymentStatus = PaymentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }
}
