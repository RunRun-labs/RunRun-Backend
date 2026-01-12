package com.multi.runrunbackend.domain.coupon.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponStatusBreakdownResDto {
    private long available;
    private long used;
    private long expired;
    
    public static CouponStatusBreakdownResDto of(
        long available,
        long used,
        long expired
    ) {
        return CouponStatusBreakdownResDto.builder()
            .available(available)
            .used(used)
            .expired(expired)
            .build();
    }
}

