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
 * @description : Please explain the class!!!
 * @filename : CouponListItemResDto
 * @since : 2025. 12. 29. Monday
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponListItemResDto {

    private Long couponId;
    private String name;
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

    public static CouponListItemResDto from(Coupon c) {
        return CouponListItemResDto.builder()
            .couponId(c.getId())
            .name(c.getName())
            .code(c.getCode())
            .quantity(c.getQuantity())
            .issuedCount(c.getIssuedCount())
            .codeType(c.getCodeType())
            .channel(c.getChannel())
            .benefitType(c.getBenefitType())
            .benefitValue(c.getBenefitValue())
            .startAt(c.getStartAt())
            .endAt(c.getEndAt())
            .status(c.getStatus())
            .createdAt(c.getCreatedAt())
            .build();
    }
}
