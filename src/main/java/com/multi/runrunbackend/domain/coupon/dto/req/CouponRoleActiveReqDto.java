package com.multi.runrunbackend.domain.coupon.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponRoleActiveReqDto
 * @since : 2025. 12. 29. Monday
 */
@Getter
public class CouponRoleActiveReqDto {

    @NotNull
    private Boolean isActive;
}