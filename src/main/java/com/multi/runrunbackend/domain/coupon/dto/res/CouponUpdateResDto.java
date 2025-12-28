package com.multi.runrunbackend.domain.coupon.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponUpdateResDto
 * @since : 2025. 12. 29. Monday
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUpdateResDto {

    private Long couponId;

    public static CouponUpdateResDto of(Long couponId) {
        return new CouponUpdateResDto(couponId);
    }

}
