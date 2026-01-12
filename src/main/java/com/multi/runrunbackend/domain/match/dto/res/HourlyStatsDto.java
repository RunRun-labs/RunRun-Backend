package com.multi.runrunbackend.domain.match.dto.res;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HourlyStatsDto {
    private int hour;                  // 0-23시
    private long count;                // 러닝 횟수
    private BigDecimal totalDistance;  // 총 거리 (km)
}
