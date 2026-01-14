package com.multi.runrunbackend.domain.match.dto.res;

import com.multi.runrunbackend.domain.match.constant.RunningType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RunningTypeBreakdownDto {
    private RunningType type;
    private long count;
    private double percentage;
}
