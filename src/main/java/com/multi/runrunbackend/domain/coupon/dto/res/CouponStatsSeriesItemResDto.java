package com.multi.runrunbackend.domain.coupon.dto.res;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponStatsSeriesItemResDto {
    private LocalDate date;
    private long issued;
    private long used;
    private long expired;
    
    public static CouponStatsSeriesItemResDto of(
        LocalDate date,
        long issued,
        long used,
        long expired
    ) {
        return CouponStatsSeriesItemResDto.builder()
            .date(date)
            .issued(issued)
            .used(used)
            .expired(expired)
            .build();
    }
}

