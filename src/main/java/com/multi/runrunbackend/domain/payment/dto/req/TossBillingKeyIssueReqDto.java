package com.multi.runrunbackend.domain.payment.dto.req;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 토스 빌링키 발급 요청 DTO
 * @filename : TossBillingKeyIssueReqDto
 * @since : 2026. 1. 1.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TossBillingKeyIssueReqDto {
    @NotBlank(message = "카드 인증 키는 필수입니다")
    private String authKey;        // 카드 인증 키 (프론트에서 받음)

    @NotBlank(message = "사용자 키는 필수입니다")
    private String customerKey;    // 사용자 키 (userId)
}
