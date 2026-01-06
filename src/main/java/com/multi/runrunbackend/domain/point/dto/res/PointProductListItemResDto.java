package com.multi.runrunbackend.domain.point.dto.res;

import com.multi.runrunbackend.domain.point.entity.PointProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : BoKyung
 * @description : 포인트 상품 목록 조회 시 개별 상품 정보를 담는 DTO
 * @filename : PointProductPageResDto
 * @since : 2026. 01. 06. 화요일
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PointProductListItemResDto {

    private Long productId;
    private String productName;
    private String productDescription;
    private Integer requiredPoint;
    private String productImageUrl;
    private Boolean isAvailable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PointProductListItemResDto from(PointProduct p, String resolvedImageUrl) {
        return PointProductListItemResDto.builder()
                .productId(p.getId())
                .productName(p.getProductName())
                .productDescription(p.getProductDescription())
                .requiredPoint(p.getRequiredPoint())
                .productImageUrl(resolvedImageUrl)
                .isAvailable(p.getIsAvailable())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
