package com.multi.runrunbackend.domain.match.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeeklyTrendDto {
    private LocalDate weekStart;       // 주 시작일 (월요일)
    private long count;                // 러닝 횟수
    private BigDecimal totalDistance;  // 총 거리 (km)
    private long totalTime;            // 총 시간 (초)
}
