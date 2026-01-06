package com.multi.runrunbackend.domain.coupon.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponRedeemReqDto
 * @since : 2025. 12. 29. Monday
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponRedeemReqDto {

    @NotBlank(message = "코드는 필수입니다.")
    private String code;
}