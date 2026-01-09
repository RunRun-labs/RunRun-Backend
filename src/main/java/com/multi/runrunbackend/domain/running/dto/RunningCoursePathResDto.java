package com.multi.runrunbackend.domain.running.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 세션 기준 코스 경로 응답 DTO
 * - fullPath: 코스 원본 LineString (GeoJSON)
 * - remainingPath: hostMatchedDistM 기준으로 잘라낸 "남은" LineString (GeoJSON)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunningCoursePathResDto {
    private Long courseId;
    private Map<String, Object> fullPath;
    private Map<String, Object> remainingPath;
    private Double startLat;
    private Double startLng;
    private Integer distanceM;
    private Double hostMatchedDistM;
}


