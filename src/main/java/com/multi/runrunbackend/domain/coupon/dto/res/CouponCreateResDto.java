package com.multi.runrunbackend.domain.coupon.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponCreateResDto
 * @since : 2025. 12. 27. Saturday
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponCreateResDto {

    private Long couponId;

    public static CouponCreateResDto of(Long couponId) {
        return new CouponCreateResDto(couponId);
    }

}
