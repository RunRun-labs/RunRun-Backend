package com.multi.runrunbackend.domain.match.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.dto.res.TodayRunningSummaryResDto;
import com.multi.runrunbackend.domain.match.dto.res.WeeklyRunningSummaryResDto;
import com.multi.runrunbackend.domain.match.service.RunningSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author : kimyongwon
 * @description : 러닝 요약 정보 조회 Controller
 * @filename : RunningSummaryController
 * @since : 26. 1. 4. 오후 5:22 일요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/summary")
public class RunningSummaryController {

    private final RunningSummaryService runningSummaryService;

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<TodayRunningSummaryResDto>> today(
            @AuthenticationPrincipal CustomUser principal
    ) {
        RunningSummaryService.TodaySummaryResult result =
                runningSummaryService.getTodaySummary(principal);

        TodayRunningSummaryResDto resDto =
                TodayRunningSummaryResDto.from(
                        result.distance(),
                        result.time(),
                        result.calories()
                );

        return ResponseEntity.ok(
                ApiResponse.success("오늘 러닝 요약 조회 성공", resDto)
        );
    }

    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<WeeklyRunningSummaryResDto>> weekly(
            @AuthenticationPrincipal CustomUser principal,
            @RequestParam(defaultValue = "0") int weekOffset
    ) {
        RunningSummaryService.WeeklySummaryResult result =
                runningSummaryService.getWeeklySummary(principal, weekOffset);

        WeeklyRunningSummaryResDto resDto =
                WeeklyRunningSummaryResDto.from(
                        result.dailyDistances(),
                        result.totalDistance(),
                        result.totalTime()
                );

        return ResponseEntity.ok(
                ApiResponse.success("주간 러닝 요약 조회 성공", resDto)
        );
    }
}