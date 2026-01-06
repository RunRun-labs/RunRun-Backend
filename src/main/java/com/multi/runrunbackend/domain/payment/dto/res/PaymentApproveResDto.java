package com.multi.runrunbackend.domain.payment.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 결제 승인 응답 DTO
 * @filename : PaymentApproveResDto
 * @since : 2026. 1. 1.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentApproveResDto {
    private String orderId;
    private String paymentKey;
    private Integer amount;
    private String status;
    private String approvedAt;
}
