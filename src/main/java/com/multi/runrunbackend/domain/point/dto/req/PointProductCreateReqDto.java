package com.multi.runrunbackend.domain.point.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 포인트 상품 생성 요청 정보를 담는 DTO
 * @filename : PointProductCreateReqDto
 * @since : 2026. 01. 06. 화요일
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointProductCreateReqDto {
    @NotBlank(message = "상품명은 필수입니다")
    @Size(min = 2, max = 100, message = "상품명은 2~100자여야 합니다")
    private String productName;

    @NotBlank(message = "상품 설명은 필수입니다")
    @Size(min = 10, max = 1000, message = "상품 설명은 10~1000자여야 합니다")
    private String productDescription;

    @NotNull(message = "필요 포인트는 필수입니다")
    @Min(value = 1, message = "필요 포인트는 1 이상이어야 합니다")
    private Integer requiredPoint;

    @NotBlank(message = "상품 이미지는 필수입니다")
    private String productImageUrl;

    private Boolean isAvailable = true;
}
