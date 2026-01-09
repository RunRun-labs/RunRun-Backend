package com.multi.runrunbackend.domain.running.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.running.dto.FreeRunCoursePreviewResDto;
import com.multi.runrunbackend.domain.running.dto.RunningCoursePathResDto;
import com.multi.runrunbackend.domain.running.dto.RunningStatsDTO;
import com.multi.runrunbackend.domain.running.service.RunningTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/running")
public class RunningRestController {

    private final RunningTrackingService runningTrackingService;

    @GetMapping("/sessions/{sessionId}/stats")
    public ResponseEntity<ApiResponse<RunningStatsDTO>> getLatestStats(
        @PathVariable Long sessionId,
        @AuthenticationPrincipal CustomUser principal
    ) {
        RunningStatsDTO stats = runningTrackingService.getLatestRunningStats(sessionId, principal);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 세션 기준 코스 경로 조회 - 재진입 시 "지나간 만큼"이 이미 반영된 remainingPath를 내려준다.
     */
    @GetMapping("/sessions/{sessionId}/course-path")
    public ResponseEntity<ApiResponse<RunningCoursePathResDto>> getSessionCoursePath(
        @PathVariable Long sessionId,
        @AuthenticationPrincipal CustomUser principal
    ) {
        RunningCoursePathResDto res = runningTrackingService.getSessionCoursePath(principal,
            sessionId);
        return ResponseEntity.ok(ApiResponse.success("세션 코스 경로 조회 성공", res));
    }

    /**
     * 자유러닝(코스 없음) 종료 후: 방장 GPS 트랙 기반 코스 프리뷰 생성
     */
    @PostMapping("/sessions/{sessionId}/free-run/course/preview")
    public ResponseEntity<ApiResponse<FreeRunCoursePreviewResDto>> previewFreeRunCourse(
        @PathVariable Long sessionId,
        @AuthenticationPrincipal CustomUser principal
    ) {
        FreeRunCoursePreviewResDto res = runningTrackingService.previewFreeRunCourse(principal,
            sessionId);
        return ResponseEntity.ok(ApiResponse.success("자유러닝 코스 프리뷰 생성 성공", res));
    }
}



