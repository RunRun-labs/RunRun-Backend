package com.multi.runrunbackend.domain.match.controller;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.dto.res.BattleResultDetailResDto;
import com.multi.runrunbackend.domain.match.dto.res.BattleResultResDto;
import com.multi.runrunbackend.domain.match.dto.res.OnlineBattleRankingResDto;
import com.multi.runrunbackend.domain.match.service.BattleResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : BattlerResultController
 * @since : 2026-01-03 토요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/battle-result")
public class BattleResultController {

    private final BattleResultService battleResultService;

    @GetMapping("/results")
    public ResponseEntity<ApiResponse<Slice<BattleResultResDto>>> getMyBattleResults(
            @AuthenticationPrincipal CustomUser principal,
            @RequestParam DistanceType distanceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        Slice<BattleResultResDto> res = battleResultService.getMyBattleResults(principal, distanceType,
                from, to, pageable);

        return ResponseEntity.ok(ApiResponse.success("내 배틀 결과 목록 조회 성공", res));
    }

    @GetMapping("/sessions/{sessionId}/results")
    public ResponseEntity<ApiResponse<BattleResultDetailResDto>> getSessionBattleResults(
            @AuthenticationPrincipal CustomUser principal,
            @PathVariable Long sessionId
    ) {

        BattleResultDetailResDto res = battleResultService.getSessionBattleResults(sessionId,
                principal);

        return ResponseEntity.ok(ApiResponse.success("세션 배틀 결과 조회 성공", res));
    }

    @GetMapping("/running-results/{runningResultId}/ranking")
    public ResponseEntity<ApiResponse<OnlineBattleRankingResDto>> getOnlineBattleRankingByRunningResultId(
            @AuthenticationPrincipal CustomUser principal,
            @PathVariable Long runningResultId
    ) {

        OnlineBattleRankingResDto res = battleResultService.getOnlineBattleRankingByRunningResultId(
                principal, runningResultId);

        return ResponseEntity.ok(ApiResponse.success("온라인배틀 등수 조회 성공", res));
    }

}
