package com.multi.runrunbackend.domain.running.ghost.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.running.ghost.service.GhostRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author : chang
 * @description : 고스트런 REST API
 * @filename : GhostRunController
 * @since : 2026-01-01
 */
@RestController
@RequestMapping("/api/ghost-run")
@RequiredArgsConstructor
@Slf4j
public class GhostRunController {

    private final GhostRunService ghostRunService;

    /**
     * 고스트 세션 종료
     * 
     * POST /api/ghost-run/{sessionId}/end
     */
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<ApiResponse<Void>> endSession(
            @PathVariable Long sessionId
    ) {
        ghostRunService.endGhostSession(sessionId);
        
        return ResponseEntity.ok(
                ApiResponse.success("고스트 세션 종료", null)
        );
    }
}
