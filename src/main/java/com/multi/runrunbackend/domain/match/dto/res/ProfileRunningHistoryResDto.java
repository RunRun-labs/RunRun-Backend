package com.multi.runrunbackend.domain.match.dto.res;

import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.course.entity.Course;
import com.multi.runrunbackend.domain.match.constant.RunningType;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author : kimyongwon
 * @description : 마이페이지 러닝 기록 응답 DTO
 * @filename : MyPageRunningRecordResDto
 * @since : 26. 1. 4. 오후 7:18 일요일
 */
@Slf4j
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

    //  온라인 배틀 전용(ONLINEBATTLE이 아닌 경우 null)
    private Integer onlineBattleRanking;

    public static ProfileRunningHistoryResDto from(RunningResult r, FileStorage fileStorage) {
        String thumbnailUrl = null;
        Course c = null;

        // ✅ Course 접근을 try-catch로 보호 (데이터 불일치 상황 대응)
        try {
            c = r.getCourse();
            // Course가 존재하는지 확인 (프록시 초기화 시도)
            if (c != null) {
                // 프록시 초기화를 위한 더미 접근
                c.getId(); // ID 접근으로 프록시 초기화 확인
            }
        } catch (EntityNotFoundException e) {
            // Course가 존재하지 않으면 null로 처리 (데이터 불일치 상황)
            log.warn("[COURSE] Course 엔티티를 찾을 수 없습니다. RunningResult ID: {}, User ID: {}, 에러: {}",
                r.getId(), r.getUser() != null ? r.getUser().getId() : "unknown", e.getMessage());
            c = null;
        }

        // 고스트런과 온라인배틀은 코스 섬네일 대신 특별 이미지 사용
        // JavaScript에서 처리하므로 null로 설정
        if (r.getRunningType().equals(RunningType.GHOST) || r.getRunningType()
            .equals(RunningType.ONLINEBATTLE)) {
            thumbnailUrl = null;
        } else {
            if (c != null && c.getThumbnailUrl() != null) {
                thumbnailUrl = fileStorage.toHttpsUrl(c.getThumbnailUrl());
            } else {
                thumbnailUrl = null;
            }
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