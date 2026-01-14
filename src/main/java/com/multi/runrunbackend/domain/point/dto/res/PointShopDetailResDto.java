package com.multi.runrunbackend.domain.point.dto.res;

import lombok.Builder;
import lombok.Getter;

/**
 * @author : BoKyung
 * @description : 포인트 상품 상세 응답 DTO
 * @filename : PointShopDetailResDto
 * @since : 2026. 01. 02. 금요일
 */
@Getter
@Builder
public class PointShopDetailResDto {
    private Long productId;
    private String productName;
    private String productDescription;
    private Integer requiredPoint;
    private String productImageUrl;
    private Boolean isAvailable;
    private Integer myPoints;
    private Boolean canPurchase;
}
