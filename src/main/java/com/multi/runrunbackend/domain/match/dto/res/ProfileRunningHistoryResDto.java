package com.multi.runrunbackend.domain.match.dto.res;

import com.multi.runrunbackend.common.file.storage.FileStorage;
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

    private BigDecimal totalDistanceKm;
    private Integer totalTimeSec;
    private BigDecimal avgPace;

    private RunningType runningType;
    private String runningTypeDescription;

    private Long courseId;
    private String courseTitle;
    private String courseThumbnailUrl;

    private String runStatus;
    private String runStatusDescription;

    // ✅ 온라인 배틀 전용(ONLINEBATTLE이 아닌 경우 null)
    private Integer onlineBattleRanking;

    public static ProfileRunningHistoryResDto from(RunningResult r, FileStorage fileStorage) {
        // 고스트는 보류: 기존과 동일하게 코스가 없으면 null로 내려감
        Course c = r.getCourse();

        String thumbnailUrl = null;
        if (c != null && c.getThumbnailUrl() != null) {
            thumbnailUrl = fileStorage.toHttpsUrl(c.getThumbnailUrl());
        }

        return ProfileRunningHistoryResDto.builder()
                .runningResultId(r.getId())
                .startedAt(r.getStartedAt())
                .totalDistanceKm(r.getTotalDistance())
                .totalTimeSec(r.getTotalTime())
                .avgPace(r.getAvgPace())
                .runningType(r.getRunningType())
                .runningTypeDescription(r.getRunningType().getDescription())
                .courseId(c != null ? c.getId() : null)
                .courseTitle(c != null ? c.getAddress() : null)
                .courseThumbnailUrl(thumbnailUrl)
                .runStatus(r.getRunStatus().name())
                .runStatusDescription(r.getRunStatus().getDescription())
                .onlineBattleRanking(null)
                .build();
    }

    /**
     * onlineBattleRanking까지 주입하는 오버로드
     */
    public static ProfileRunningHistoryResDto from(
            RunningResult r,
            FileStorage fileStorage,
            Integer onlineBattleRanking
    ) {
        ProfileRunningHistoryResDto base = from(r, fileStorage);

        return ProfileRunningHistoryResDto.builder()
                .runningResultId(base.getRunningResultId())
                .startedAt(base.getStartedAt())
                .totalDistanceKm(base.getTotalDistanceKm())
                .totalTimeSec(base.getTotalTimeSec())
                .avgPace(base.getAvgPace())
                .runningType(base.getRunningType())
                .runningTypeDescription(base.getRunningTypeDescription())
                .courseId(base.getCourseId())
                .courseTitle(base.getCourseTitle())
                .courseThumbnailUrl(base.getCourseThumbnailUrl())
                .runStatus(base.getRunStatus())
                .runStatusDescription(base.getRunStatusDescription())
                .onlineBattleRanking(onlineBattleRanking)
                .build();
    }
}