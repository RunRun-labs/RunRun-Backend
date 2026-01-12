package com.multi.runrunbackend.domain.match.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CourseStatsDto {
    private Long courseId;      // 코스 ID
    private String courseName;  // 코스명
    private long usageCount;    // 사용 횟수
    private double percentage;  // 사용 비율 (%)
}
