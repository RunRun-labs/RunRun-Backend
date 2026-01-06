package com.multi.runrunbackend.domain.payment.dto.res;

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

    /**
     * @description : Entity를 DTO로 변환
     */
    public static PaymentHistoryResDto fromEntity(Payment payment) {
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
                .build();
    }
}
