package com.multi.runrunbackend.domain.payment.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 결제 요청 생성 응답 DTO
 * @filename : PaymentRequestResDto
 * @since : 2026. 1. 1.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentRequestResDto {
    private String orderId;
    private Integer amount;
    private String orderName;
    private String customerName;
    private String customerEmail;
    private String customerKey;
}
