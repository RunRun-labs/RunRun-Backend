package com.multi.runrunbackend.domain.point.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * @author : BoKyung
 * @description : 포인트 메인 화면 응답 DTO
 * @filename : PointMainResDto
 * @since : 2026. 01. 02. 금요일
 */
@Getter
@Builder
public class PointMainResDto {

    private Integer availablePoints;  // 사용 가능 포인트
    private List<PointEarnMethod> earnMethods;  // 포인트 적립 방법
    private UpcomingExpiryInfo upcomingExpiry;  // 소멸 예정 포인트
    private PointSummary summary;

    @Getter
    @Builder
    public static class PointEarnMethod {
        private String methodName;
        private String description;
        private Integer earnAmount;
    }

    @Getter
    @Builder
    public static class UpcomingExpiryInfo {
        private String expiryDate;
        private Integer expiringPoints;
    }

    @Getter
    @Builder
    public static class PointSummary {
        private Integer earnedPoints;  // 적립
        private Integer usedPoints;  // 사용
        private Integer totalAccumulated;  // 총 적립 포인트
    }

}
