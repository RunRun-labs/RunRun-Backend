package com.multi.runrunbackend.domain.admin.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopCouponResDto {
    private Long couponId;
    private String couponName;
    private long issuedCount;
    
    public static TopCouponResDto of(Long couponId, String couponName, long issuedCount) {
        return TopCouponResDto.builder()
            .couponId(couponId)
            .couponName(couponName)
            .issuedCount(issuedCount)
            .build();
    }
}

