package com.multi.runrunbackend.domain.coupon.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponRedeemReqDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponIssueListItemResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponIssueListReqDto;
import com.multi.runrunbackend.domain.coupon.service.CouponIssueService;
import com.multi.runrunbackend.domain.course.dto.req.CursorPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponIssueController
 * @since : 2025. 12. 29. Monday
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupon-issues")
public class CouponIssueController {

    private final CouponIssueService couponIssueService;

    @PostMapping("/redeem")
    public ResponseEntity<ApiResponse<Void>> createCouponIssue(
        @AuthenticationPrincipal CustomUser principal,
        @Valid @RequestBody CouponRedeemReqDto req
    ) {
        couponIssueService.createCouponIssue(principal, req);
        return ResponseEntity.ok(
            ApiResponse.successNoData("쿠폰 코드 입력 성공"));
    }

    @PostMapping("/{coupon_id}/download")
    public ResponseEntity<ApiResponse<Void>> download(
        @AuthenticationPrincipal CustomUser principal,
        @PathVariable(name = "coupon_id") Long couponId
    ) {
        couponIssueService.download(principal, couponId);
        return ResponseEntity.ok(ApiResponse.successNoData("쿠폰 다운로드 성공"
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPage<CouponIssueListItemResDto>>> getCouponIssueList(
        @AuthenticationPrincipal CustomUser principal,
        @ModelAttribute CouponIssueListReqDto req
    ) {
        CursorPage<CouponIssueListItemResDto> list = couponIssueService.getCouponIssueList(
            principal, req);
        return ResponseEntity.ok(ApiResponse.success("쿠폰 목록 조회 성공", list));
    }

    @DeleteMapping("/{couponIssue_id}")
    public ResponseEntity<ApiResponse<Void>> deleteCouponIssue(
        @PathVariable(name = "couponIssue_id") Long couponIssueId,
        @AuthenticationPrincipal CustomUser principal
    ) {
        couponIssueService.deleteCouponIssue(couponIssueId, principal);
        return ResponseEntity.ok(ApiResponse.successNoData("쿠폰 삭제 성공"));

    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCouponCount(
        @AuthenticationPrincipal CustomUser principal
    ) {
        long count = couponIssueService.getCouponCount(principal);
        return ResponseEntity.ok(ApiResponse.success("쿠폰 개수 조회 성공", count));
    }


}
