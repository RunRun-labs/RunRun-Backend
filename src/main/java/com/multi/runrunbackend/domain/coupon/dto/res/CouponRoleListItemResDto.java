package com.multi.runrunbackend.domain.coupon.dto.res;

import com.multi.runrunbackend.domain.coupon.constant.CouponTriggerEvent;
import com.multi.runrunbackend.domain.coupon.entity.CouponRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponRoleListItemResDto
 * @since : 2025. 12. 29. Monday
 */
@Getter
@Builder
public class CouponRoleListItemResDto {

    private Long id;
    private String name;
    private Long couponId;
    private String couponName;
    private CouponTriggerEvent triggerEvent;
    private Integer conditionValue;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static CouponRoleListItemResDto from(CouponRole role) {
        return CouponRoleListItemResDto.builder()
            .id(role.getId())
            .name(role.getName())
            .couponId(role.getCoupon().getId())
            .couponName(role.getCoupon().getName())
            .triggerEvent(role.getTriggerEvent())
            .conditionValue(role.getConditionValue())
            .isActive(role.getIsActive())
            .createdAt(role.getCreatedAt())
            .build();
    }
}