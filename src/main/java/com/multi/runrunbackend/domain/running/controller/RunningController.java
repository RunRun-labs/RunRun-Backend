package com.multi.runrunbackend.domain.running.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.running.service.RunningTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 런닝 REST Controller
 * - 런닝 종료 처리
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
     * 런닝 종료 (오프라인)
     * - 방장이 호출
     * - Redis 데이터 → PostgreSQL 저장
     * - 모든 참여자에게 동일한 기록 저장
     * 
     * POST /api/running/sessions/{sessionId}/finish
     * 
     * @param sessionId 세션 ID
     * @param principal 현재 사용자 (방장)
     * @return 성공 응답
     */
    @PostMapping("/sessions/{sessionId}/finish")
    public ResponseEntity<ApiResponse<Void>> finishRunning(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUser principal) {
        
        log.info("🏁 런닝 종료 요청: sessionId={}, loginId={}", sessionId, principal.getLoginId());
        
        // principal에서 loginId를 가져와서 Service에 전달
        trackingService.finishOfflineRunning(sessionId, principal.getLoginId());
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    /**
     * 런닝 결과 조회
     * - 런닝 종료 후 결과 데이터 조회
     * 
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
        
        Object result = trackingService.getRunningResult(sessionId, principal.getLoginId());
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
