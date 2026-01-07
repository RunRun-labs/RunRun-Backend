package com.multi.runrunbackend.domain.coupon.dto.res;

import com.multi.runrunbackend.domain.coupon.constant.CouponTriggerEvent;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponRoleListReqDto
 * @since : 2025. 12. 29. Monday
 */
@Getter
@Setter
public class CouponRoleListReqDto {

    private Long couponId;

    private List<CouponTriggerEvent> triggerEvents;

    private Boolean isActive;
    
    private String keyword;
}
