package com.multi.runrunbackend.domain.payment.dto.res;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 토스 빌링키 발급 응답 DTO
 * @filename : TossBillingKeyResDto
 * @since : 2026. 1. 1.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossBillingKeyResDto {
    private String billingKey;
    private String customerKey;
    private String authenticatedAt;
    private Card card;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Card {
        private String issuerCode;
        private String acquirerCode;
        private String number;
        private String cardType;
        private String ownerType;
    }
}
