package com.multi.runrunbackend.domain.payment.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 결제 승인 요청 DTO (프론트 → 백엔드, 백엔드 → 토스 둘 다 사용)
 * @filename : PaymentApproveReqDto
 * @since : 2026. 1. 1.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class PaymentApproveReqDto {


    @NotBlank(message = "주문 ID는 필수입니다")
    private String orderId;

    @NotBlank(message = "결제 키는 필수입니다")
    private String paymentKey;

    @NotNull(message = "결제 금액은 필수입니다")
    private Integer amount;
}
