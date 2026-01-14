package com.multi.runrunbackend.domain.point.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 포인트 상품 수정 결과를 반환하는 DTO
 * @filename : PointProductUpdateResDto
 * @since : 2026. 01. 06. 화요일
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointProductUpdateResDto {
    private Long productId;

    public static PointProductUpdateResDto of(Long productId) {
        return new PointProductUpdateResDto(productId);
    }
}
