package com.multi.runrunbackend.domain.coupon.dto.res;

import com.multi.runrunbackend.domain.coupon.constant.CouponBenefitType;
import com.multi.runrunbackend.domain.coupon.constant.CouponChannel;
import com.multi.runrunbackend.domain.coupon.constant.CouponCodeType;
import com.multi.runrunbackend.domain.coupon.constant.CouponStatus;
import com.multi.runrunbackend.domain.coupon.entity.Coupon;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : 쿠폰 상세 정보 응답 DTO
 * @filename : CouponDetailResDto
 * @since : 2025. 12. 29. Monday
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponDetailResDto {

    private Long couponId;
    private String name;
    private String description;
    private String code;
    private Integer quantity;
    private Integer issuedCount;
    private CouponCodeType codeType;
    private CouponChannel channel;
    private CouponBenefitType benefitType;
    private Integer benefitValue;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private CouponStatus status;
    private LocalDateTime createdAt;

    public static CouponDetailResDto from(Coupon coupon) {
        return CouponDetailResDto.builder()
            .couponId(coupon.getId())
            .name(coupon.getName())
            .description(coupon.getDescription())
            .code(coupon.getCode())
            .quantity(coupon.getQuantity())
            .issuedCount(coupon.getIssuedCount())
            .codeType(coupon.getCodeType())
            .channel(coupon.getChannel())
            .benefitType(coupon.getBenefitType())
            .benefitValue(coupon.getBenefitValue())
            .startAt(coupon.getStartAt())
            .endAt(coupon.getEndAt())
            .status(coupon.getStatus())
            .createdAt(coupon.getCreatedAt())
            .build();
    }
}

