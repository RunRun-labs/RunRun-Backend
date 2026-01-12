package com.multi.runrunbackend.domain.match.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaceDistributionDto {
    private String paceRange;  // 페이스 구간 (예: "3:00-4:00", "4:00-5:00" 등)
    private long userCount;    // 해당 구간의 유저 수
}
