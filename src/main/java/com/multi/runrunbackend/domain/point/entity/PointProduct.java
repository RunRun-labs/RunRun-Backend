package com.multi.runrunbackend.domain.point.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 포인트 상품 엔티티
 * @filename : PointProduct
 * @since : 25. 12. 17. 수요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PointProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", length = 100)
    private String productName;

    @Column(name = "required_point", nullable = false)
    private Integer requiredPoint;

    @Column(name = "product_image_url", nullable = false, length = 500)
    private String productImageUrl;

    @Column(name = "product_description", nullable = false, columnDefinition = "TEXT")
    private String productDescription;

    @Column(name = "is_available")
    private Boolean isAvailable;

    /**
     * @description : toEntity - 엔티티 생성 정적 팩토리 메서드
     * @filename : PointProduct
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public static PointProduct toEntity(String productName, Integer requiredPoint,
        String productImageUrl, String productDescription) {
        return PointProduct.builder()
            .productName(productName)
            .requiredPoint(requiredPoint)
            .productImageUrl(productImageUrl)
            .productDescription(productDescription)
            .isAvailable(true)
            .build();
    }

    /**
     * @description : updateAvailability - 상품 판매 가능 여부 변경
     * @filename : PointProduct
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void updateAvailability(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
}
