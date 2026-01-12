package com.multi.runrunbackend.domain.admin.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.admin.dto.res.DashboardStatsResDto;
import com.multi.runrunbackend.domain.admin.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResDto>> getDashboardStats() {
        return ResponseEntity.ok(
            ApiResponse.success("대시보드 통계 조회 성공", dashboardService.getDashboardStats())
        );
    }
}

