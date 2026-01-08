package com.multi.runrunbackend.domain.point.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * @author : BoKyung
 * @description : 포인트 상점 목록 응답 DTO
 * @filename : PointShopListResDto
 * @since : 2026. 01. 02. 금요일
 */
@Getter
@Builder
public class PointShopListResDto {

    private Integer myPoints;
    private List<ShopItemDto> products;  // 상품 목록

    @Getter
    @Builder
    public static class ShopItemDto {
        private Long productId;
        private String productName;
        private Integer requiredPoint;
        private String productImageUrl;
        private Boolean canPurchase;  // 구매 가능 여부(보유 포인트 >= 구매 포인트)
    }
}
