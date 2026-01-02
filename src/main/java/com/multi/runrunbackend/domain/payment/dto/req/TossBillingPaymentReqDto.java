package com.multi.runrunbackend.domain.payment.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 토스 빌링키 결제 요청 DTO
 * @filename : TossBillingPaymentReqDto
 * @since : 2026. 1. 1.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TossBillingPaymentReqDto {

    private String customerKey;
    private Integer amount;
    private String orderId;
    private String orderName;
    private String customerEmail;
    private String customerName;
}
