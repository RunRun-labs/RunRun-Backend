package com.multi.runrunbackend.domain.match.dto.res;

import com.multi.runrunbackend.domain.match.constant.Tier;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TierPaceDto {
    private Tier tier;
    private BigDecimal avgPace;  // 평균 페이스 (분/km)
    private long sampleCount;     // 샘플 수
}
