package com.multi.runrunbackend.domain.payment.dto.res;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 토스페이먼츠 응답 DTO
 * @filename : TossPaymentResDto
 * @since : 2026. 1. 1.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TossPaymentResDto {
    private String paymentKey;
    private String orderId;
    private String orderName;
    private String status;
    private Integer totalAmount;
    private String method;          // 결제 수단
    private String approvedAt;
    private String canceledAt;

    // 빌링키 관련
    private Card card;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Card {
        private String issuerCode;
        private String acquirerCode;
        private String number;
        private String billingKey;  // 빌링키!
    }
}
