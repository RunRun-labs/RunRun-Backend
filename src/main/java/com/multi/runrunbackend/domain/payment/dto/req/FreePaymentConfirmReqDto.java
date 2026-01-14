package com.multi.runrunbackend.domain.payment.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : /confirm-free 전용 DTO
 * @filename : FreePaymentConfirmReqDto
 * @since : 2026. 1. 2.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreePaymentConfirmReqDto {
    @NotBlank(message = "주문 ID는 필수입니다")
    private String orderId;
}
