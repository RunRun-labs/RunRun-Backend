package com.multi.runrunbackend.domain.point.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.point.dto.req.PointProductUpdateReqDto;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author : BoKyung
 * @description : 포인트 상품 엔티티
 * @filename : PointProduct
 * @since : 25. 12. 17. 수요일
 */
@Entity
@Table(name = "point_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PointProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false, length = 100)
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

    public void update(PointProductUpdateReqDto req) {
        this.productName = req.getProductName();
        this.productDescription = req.getProductDescription();
        this.requiredPoint = req.getRequiredPoint();
        this.productImageUrl = req.getProductImageUrl();
        this.isAvailable = req.getIsAvailable();
    }

    public void delete() {
        this.isDeleted = true;
    }

}
