package com.multi.runrunbackend.domain.match.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.match.dto.res.RunningStatsResDto;
import com.multi.runrunbackend.domain.match.service.RunningStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : kyungsoo
 * @description : 러닝 통계 컨트롤러
 * @filename : RunningStatsController
 * @since : 2026. 1. 15. Wednesday
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/running/stats")
@PreAuthorize("hasRole('ADMIN')")
public class RunningStatsController {

    private final RunningStatsService runningStatsService;

    @GetMapping
    public ResponseEntity<ApiResponse<RunningStatsResDto>> getRunningStats() {
        RunningStatsResDto stats = runningStatsService.getRunningStats();
        return ResponseEntity.ok(
            ApiResponse.success("러닝 통계 조회 성공", stats)
        );
    }
}
