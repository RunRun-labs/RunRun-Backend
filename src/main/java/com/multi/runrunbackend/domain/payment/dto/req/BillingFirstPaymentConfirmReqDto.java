package com.multi.runrunbackend.domain.payment.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author : BoKyung
 * @description : /billing/confirm 전용 DTO
 * @filename : FreePaymentConfirmReqDto
 * @since : 2026. 1. 2.
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BillingFirstPaymentConfirmReqDto {
    @ToString.Exclude
    @NotBlank(message = "customerKey는 필수입니다")
    private String customerKey;

    @NotBlank(message = "authKey는 필수입니다")
    private String authKey;

    @NotBlank(message = "주문 ID는 필수입니다")
    private String orderId;

    @NotNull(message = "결제 금액은 필수입니다")
    @Min(value = 0, message = "결제 금액은 0원 이상이어야 합니다")
    private Integer amount;

}
