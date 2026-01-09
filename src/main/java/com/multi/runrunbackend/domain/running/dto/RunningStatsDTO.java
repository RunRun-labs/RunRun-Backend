package com.multi.runrunbackend.domain.running.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 런닝 통계 DTO (오프라인 러닝용) - 서버 → 클라이언트 브로드캐스트용 - 방장 GPS 기준 팀 통계
 *
 * @author : chang
 * @since : 2024-12-23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)  // Redis @class 필드 무시
public class RunningStatsDTO {

    private Long sessionId;

    // 팀 통계 (방장 GPS 기준)
    private Double teamAveragePace;      // 팀 평균 페이스 (분/km)
    private Double totalDistance;        // 현재 거리 (km)
    private Double remainingDistance;    // 남은 거리 (km)
    private Integer totalRunningTime;    // 총 런닝 시간 (초)

    // 구간별 팀 평균 페이스
    // 예: {1: 5.5, 2: 5.3, 3: 5.1} → 0~1km: 5분30초/km, 0~2km: 5분18초/km
    private Map<Integer, Double> segmentPaces;

    private Double hostLatitude;
    private Double hostLongitude;
    private Double hostHeading;

    /**
     * 방장 코스 위 매칭 진행도 (미터) - 참여자도 방장과 동일하게 "선 위를 지날 때만" 코스 선이 지워지게 하기 위한 값
     */
    private Double hostMatchedDistM;

    // 이벤트 정보
    private Integer kmReached;           // 새로 도달한 km (1, 2, 3...) - 1km 도달 시 알림용
    private boolean isCompleted;         // 목표 거리 완주 여부 - 자동 종료용

    // 타임스탬프
    private Long timestamp;
}
