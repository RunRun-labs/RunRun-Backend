package com.multi.runrunbackend.domain.point.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 포인트 사용 요청 DTO
 * @filename : PointUseReqDto
 * @since : 2026. 01. 02. 금요일
 */
@Getter
@NoArgsConstructor
public class PointUseReqDto {

    @NotNull(message = "사용 포인트는 필수입니다")
    @Min(value = 1, message = "사용 포인트는 1p 이상이어야 합니다")
    private Integer amount;

    @NotNull(message = "사용 사유는 필수입니다")
    private String reason;

    private Long pointProductId;
}
