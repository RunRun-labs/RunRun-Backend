package com.multi.runrunbackend.domain.payment.dto.req;

import lombok.*;

/**
 * @author : BoKyung
 * @description : 토스 빌링키 결제 요청 DTO
 * @filename : TossBillingPaymentReqDto
 * @since : 2026. 1. 1.
 */
@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TossBillingPaymentReqDto {

    @ToString.Exclude
    private String customerKey;
    private Integer amount;
    private String orderId;
    private String orderName;

    @ToString.Exclude
    private String customerEmail;

    @ToString.Exclude
    private String customerName;
}
