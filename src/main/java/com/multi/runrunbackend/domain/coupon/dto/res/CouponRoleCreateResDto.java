package com.multi.runrunbackend.domain.coupon.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponRoleCreateResDto
 * @since : 2025. 12. 29. Monday
 */
@Getter
@AllArgsConstructor
public class CouponRoleCreateResDto {

    private Long couponRoleId;

    public static CouponRoleCreateResDto of(Long couponRoleId) {
        return new CouponRoleCreateResDto(couponRoleId);

    }

}
