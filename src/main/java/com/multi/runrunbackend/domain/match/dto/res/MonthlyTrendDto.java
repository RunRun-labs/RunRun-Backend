package com.multi.runrunbackend.domain.match.dto.res;

import java.math.BigDecimal;
import java.time.YearMonth;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonthlyTrendDto {
    private YearMonth month;           // 연월
    private long count;                // 러닝 횟수
    private BigDecimal totalDistance;  // 총 거리 (km)
    private long totalTime;            // 총 시간 (초)
}
