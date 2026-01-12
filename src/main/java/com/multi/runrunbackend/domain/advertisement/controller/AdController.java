package com.multi.runrunbackend.domain.advertisement.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import com.multi.runrunbackend.domain.advertisement.dto.req.adclick.AdClickReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.adserve.AdServeResDto;
import com.multi.runrunbackend.domain.advertisement.service.AdTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : kyungsoo
 * @description : 광고 서빙 및 추적 공개 API
 * @filename : AdController
 * @since : 2026. 1. 11. Sunday
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ads")
public class AdController {

    private final AdTrackingService adTrackingService;

    /**
     * 광고 서빙 (노출 자동 카운트)
     * GET /api/ads/serve?slotType=HOME_TOP_BANNER
     */
    @GetMapping("/serve")
    public ResponseEntity<ApiResponse<AdServeResDto>> serve(
        @RequestParam AdSlotType slotType
    ) {
        AdServeResDto result = adTrackingService.serveOne(slotType);
        return ResponseEntity.ok(ApiResponse.success("광고 조회 성공", result));
    }

    /**
     * 광고 클릭 카운트
     * POST /api/ads/click
     */
    @PostMapping("/click")
    public ResponseEntity<ApiResponse<Void>> click(
        @Valid @RequestBody AdClickReqDto dto
    ) {
        adTrackingService.click(dto.getPlacementId());
        return ResponseEntity.ok(ApiResponse.successNoData("클릭 카운트 성공"));
    }
}

