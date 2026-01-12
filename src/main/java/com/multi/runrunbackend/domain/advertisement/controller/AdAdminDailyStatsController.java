package com.multi.runrunbackend.domain.advertisement.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.advertisement.constant.AdDailySort;
import com.multi.runrunbackend.domain.advertisement.constant.SortDir;
import com.multi.runrunbackend.domain.advertisement.dto.res.adstats.AdDailyStatsListItemResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.common.PageResDto;
import com.multi.runrunbackend.domain.advertisement.service.AdDailyStatsService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdAdminDailyStatsController
 * @since : 2026. 1. 11. Sunday
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/ad-placements")
public class AdAdminDailyStatsController {

    private final AdDailyStatsService adDailyStatsService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{placementId}/daily-stats")
    public ResponseEntity<ApiResponse<PageResDto<AdDailyStatsListItemResDto>>> listDailyStats(
        @PathVariable Long placementId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) AdDailySort sort,
        @RequestParam(required = false) SortDir dir,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("배치 일별 통계 조회 성공",
                adDailyStatsService.listByPlacement(placementId, from, to, sort, dir, page, size))
        );
    }
}