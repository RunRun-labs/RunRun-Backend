package com.multi.runrunbackend.domain.match.dto.res;

import com.multi.runrunbackend.domain.match.constant.Tier;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TierStatsDto {
    private Tier tier;
    private long totalCount;           // 러닝 횟수
    private BigDecimal totalDistance;  // 총 거리 (km)
}
