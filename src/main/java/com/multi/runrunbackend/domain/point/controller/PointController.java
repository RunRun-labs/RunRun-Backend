package com.multi.runrunbackend.domain.point.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.point.dto.req.CursorPage;
import com.multi.runrunbackend.domain.point.dto.req.PointEarnReqDto;
import com.multi.runrunbackend.domain.point.dto.req.PointHistoryListReqDto;
import com.multi.runrunbackend.domain.point.dto.req.PointUseReqDto;
import com.multi.runrunbackend.domain.point.dto.res.PointHistoryListResDto;
import com.multi.runrunbackend.domain.point.dto.res.PointMainResDto;
import com.multi.runrunbackend.domain.point.dto.res.PointShopDetailResDto;
import com.multi.runrunbackend.domain.point.dto.res.PointShopListResDto;
import com.multi.runrunbackend.domain.point.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * @author : BoKyung
 * @description : 포인트 컨트롤러
 * @filename : PointController
 * @since : 2026. 01. 02. 금요일
 */
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping
    public ResponseEntity<ApiResponse<PointMainResDto>> getPointMain(
            @AuthenticationPrincipal CustomUser principal
    ) {
        PointMainResDto response = pointService.getPointMain(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("포인트 메인 조회 성공", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<CursorPage<PointHistoryListResDto>>> getPointHistory(
            @AuthenticationPrincipal CustomUser principal,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String filter
    ) {
        PointHistoryListReqDto reqDto = new PointHistoryListReqDto();
        reqDto.setCursor(cursor);
        reqDto.setSize(size);
        reqDto.setFilter(filter);

        CursorPage<PointHistoryListResDto> response = pointService.getPointHistoryList(
                principal.getUserId(), reqDto
        );
        return ResponseEntity.ok(ApiResponse.success("포인트 내역 조회 성공", response));
    }

    @PostMapping("/earn")
    public ResponseEntity<ApiResponse<Void>> earnPoints(
            @AuthenticationPrincipal CustomUser principal,
            @Valid @RequestBody PointEarnReqDto requestDto
    ) {
        pointService.earnPoints(principal.getUserId(), requestDto);
        return ResponseEntity.ok(ApiResponse.successNoData("포인트 적립 성공"));
    }

    @PostMapping("/use")
    public ResponseEntity<ApiResponse<Void>> usePoints(
            @AuthenticationPrincipal CustomUser principal,
            @Valid @RequestBody PointUseReqDto requestDto
    ) {
        pointService.usePoints(principal.getUserId(), requestDto);
        return ResponseEntity.ok(ApiResponse.successNoData("포인트 사용 성공"));
    }

    @GetMapping("/shop")
    public ResponseEntity<ApiResponse<PointShopListResDto>> getPointShop(
            @AuthenticationPrincipal CustomUser principal
    ) {
        PointShopListResDto response = pointService.getPointShop(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("포인트 상점 조회 성공", response));
    }

    @GetMapping("/shop/{productId}")
    public ResponseEntity<ApiResponse<PointShopDetailResDto>> getPointShopDetail(
            @AuthenticationPrincipal CustomUser principal,
            @PathVariable Long productId
    ) {
        PointShopDetailResDto response = pointService.getPointShopDetail(
                principal.getUserId(), productId
        );
        return ResponseEntity.ok(ApiResponse.success("포인트 상품 상세 조회 성공", response));
    }

    /**
     * 내 포인트 잔액 조회
     */
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<Integer>> getMyPointBalance(
            @AuthenticationPrincipal CustomUser principal
    ) {
        Integer balance = pointService.getAvailablePoints(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("포인트 잔액 조회 성공", balance));
    }
}
