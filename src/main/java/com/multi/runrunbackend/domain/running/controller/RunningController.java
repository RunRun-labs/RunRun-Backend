package com.multi.runrunbackend.domain.running.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.running.dto.req.FinishRunningReqDto;
import com.multi.runrunbackend.domain.running.service.RunningTrackingService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 런닝 REST Controller - 런닝 종료 처리
 *
 * @author : chang
 * @since : 2024-12-23
 */
@RestController
@RequestMapping("/api/running")
@RequiredArgsConstructor
@Slf4j
public class RunningController {

    private final RunningTrackingService trackingService;

    /**
     * 런닝 종료 (오프라인) - 방장이 호출 - Redis 데이터 → PostgreSQL 저장 - 모든 참여자에게 동일한 기록 저장
     * <p>
     * POST /api/running/sessions/{sessionId}/finish
     *
     * @param sessionId 세션 ID
     * @param principal 현재 사용자 (방장)
     * @return 성공 응답
     */
    @PostMapping("/sessions/{sessionId}/finish")
    public ResponseEntity<ApiResponse<Void>> finishRunning(
        @PathVariable Long sessionId,
        @AuthenticationPrincipal CustomUser principal,
        @RequestBody(required = false) FinishRunningReqDto req
    ) {

        log.info("🏁 런닝 종료 요청: sessionId={}, loginId={}", sessionId, principal.getLoginId());

        trackingService.finishOfflineRunning(sessionId, principal.getLoginId(), req);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 런닝 결과 조회 - 런닝 종료 후 결과 데이터 조회
     * <p>
     * GET /api/running/sessions/{sessionId}/result
     *
     * @param sessionId 세션 ID
     * @param principal 현재 사용자
     * @return 런닝 결과 데이터
     */
    @GetMapping("/sessions/{sessionId}/result")
    public ResponseEntity<ApiResponse<Object>> getRunningResult(
        @PathVariable Long sessionId,
        @AuthenticationPrincipal CustomUser principal) {

        log.info("📊 런닝 결과 조회: sessionId={}, loginId={}", sessionId, principal.getLoginId());

        Map<String, Object> result = trackingService.getRunningResult(sessionId,
            principal.getLoginId());

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
