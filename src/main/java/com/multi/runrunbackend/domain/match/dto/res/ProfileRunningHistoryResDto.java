package com.multi.runrunbackend.domain.match.dto.res;

import com.multi.runrunbackend.domain.course.entity.Course;
import com.multi.runrunbackend.domain.match.constant.RunningType;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 *
 * @author : kimyongwon
 * @description : 마이페이지 러닝 기록 응답 DTO
 * @filename : MyPageRunningRecordResDto
 * @since : 26. 1. 4. 오후 7:18 일요일
 */
@Getter
@Builder
public class ProfileRunningHistoryResDto {

    private Long runningResultId;

    private LocalDateTime startedAt;

    // 거리 / 시간
    private BigDecimal totalDistanceKm;
    private Integer totalTimeSec;

    // 평균 페이스 (분/km)
    private BigDecimal avgPace;

    // 러닝 타입
    private RunningType runningType;
    private String runningTypeDescription;

    // 코스
    private Long courseId;
    private String courseTitle;
    private String courseThumbnailUrl;

    public static ProfileRunningHistoryResDto from(RunningResult r) {
        Course c = r.getCourse();

        return ProfileRunningHistoryResDto.builder()
                .runningResultId(r.getId())
                .startedAt(r.getStartedAt())
                .totalDistanceKm(r.getTotalDistance())
                .totalTimeSec(r.getTotalTime())
                .avgPace(r.getAvgPace())
                .runningType(r.getRunningType())
                .runningTypeDescription(r.getRunningType().getDescription())
                .courseId(c != null ? c.getId() : null)
                .courseTitle(c != null ? c.getTitle() : null)
                .courseThumbnailUrl(c != null ? c.getThumbnailUrl() : null)
                .build();
    }
}