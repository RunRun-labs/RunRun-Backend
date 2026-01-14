package com.multi.runrunbackend.domain.payment.dto.res;

import com.multi.runrunbackend.domain.coupon.entity.Coupon;
import com.multi.runrunbackend.domain.coupon.entity.CouponIssue;
import com.multi.runrunbackend.domain.payment.constant.PaymentMethod;
import com.multi.runrunbackend.domain.payment.constant.PaymentStatus;
import com.multi.runrunbackend.domain.payment.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : BoKyung
 * @description : 결제 내역 조회 응답 DTO
 * @filename : PaymentHistoryResDto
 * @since : 2026. 1. 1.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentHistoryResDto {
    private Long paymentId;
    private String orderName;         // "프리미엄 플랜"
    private Integer originalAmount;
    private Integer discountAmount;
    private Integer finalAmount;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private Boolean isAutoPayment;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    // 쿠폰 정보
    private CouponInfo couponInfo;

    // 카드 정보 (마스킹된 카드번호)
    private String cardInfo;

    /**
     * @description : Entity를 DTO로 변환
     */
    public static PaymentHistoryResDto fromEntity(Payment payment) {
        // 쿠폰 정보 생성
        CouponInfo couponInfo = null;
        if (payment.getCouponIssue() != null) {
            CouponIssue issue = payment.getCouponIssue();
            Coupon coupon = issue.getCoupon();

            couponInfo = CouponInfo.builder()
                    .couponCode(coupon.getCode())
                    .couponName(coupon.getName())
                    .discountAmount(payment.getDiscountAmount())
                    .build();
        }

        // 카드 정보 생성
        String cardInfo = null;
        if (payment.getPaymentMethod() != null) {
            String methodDescription = payment.getPaymentMethod().name();

            // 카드번호가 있으면 표시
            if (payment.getCardNumber() != null && !payment.getCardNumber().isBlank()) {
                cardInfo = methodDescription + " " + payment.getCardNumber();
            } else {
                cardInfo = methodDescription;
            }

            // 자동결제면 표시
            if (payment.getBillingKey() != null && !payment.getBillingKey().isBlank()) {
                cardInfo += " (자동결제)";
            }
        }

        return PaymentHistoryResDto.builder()
                .paymentId(payment.getId())
                .orderName("프리미엄 플랜")
                .originalAmount(payment.getOriginalAmount())
                .discountAmount(payment.getDiscountAmount())
                .finalAmount(payment.getFinalAmount())
                .paymentStatus(payment.getPaymentStatus())
                .paymentMethod(payment.getPaymentMethod())
                .isAutoPayment(payment.getIsAutoPayment())
                .createdAt(payment.getCreatedAt())
                .approvedAt(payment.getApprovedAt())
                .couponInfo(couponInfo)
                .cardInfo(cardInfo)
                .build();
    }

    // 쿠폰 정보
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CouponInfo {
        private String couponCode;      // 쿠폰 코드
        private String couponName;      // 쿠폰명
        private Integer discountAmount; // 할인 금액
    }
}
