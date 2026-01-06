package com.multi.runrunbackend.domain.payment.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 멤버십 결제 요청 DTO
 * @filename : PaymentRequestReqDto
 * @since : 2026. 1. 1.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestReqDto {

    private String couponCode;  // 쿠폰 코드
}
