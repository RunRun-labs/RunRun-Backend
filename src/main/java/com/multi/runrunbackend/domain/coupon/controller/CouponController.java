package com.multi.runrunbackend.domain.coupon.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.coupon.constant.CouponChannel;
import com.multi.runrunbackend.domain.coupon.constant.CouponCodeType;
import com.multi.runrunbackend.domain.coupon.constant.CouponStatus;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponCreateReqDto;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponUpdateReqDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponCreateResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponDetailResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponPageResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponStatsResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponUpdateResDto;
import com.multi.runrunbackend.domain.coupon.service.CouponService;
import com.multi.runrunbackend.domain.coupon.service.CouponStatsService;
import com.multi.runrunbackend.domain.advertisement.constant.StatsRange;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponController
 * @since : 2025. 12. 27. Saturday
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/coupons")
public class CouponController {

    private final CouponService couponService;
    private final CouponStatsService couponStatsService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{coupon_id}")
    public ResponseEntity<ApiResponse<CouponDetailResDto>> getCoupon(
        @PathVariable(name = "coupon_id") Long couponId) {

        CouponDetailResDto res = couponService.getCoupon(couponId);
        return ResponseEntity.ok(ApiResponse.success("쿠폰 상세 조회 성공", res));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{coupon_id}/stats")
    public ResponseEntity<ApiResponse<CouponStatsResDto>> getCouponStats(
        @PathVariable(name = "coupon_id") Long couponId,
        @RequestParam(required = false) StatsRange range
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("쿠폰 통계 조회 성공", couponStatsService.getCouponStats(couponId, range))
        );
    }

    @GetMapping("/public/{coupon_id}")
    public ResponseEntity<ApiResponse<CouponDetailResDto>> getPublicCoupon(
        @PathVariable(name = "coupon_id") Long couponId) {

        CouponDetailResDto res = couponService.getCoupon(couponId);
        return ResponseEntity.ok(ApiResponse.success("쿠폰 상세 조회 성공", res));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<CouponPageResDto>> listCoupons(
        @RequestParam(required = false) List<CouponStatus> statuses,
        @RequestParam(required = false) List<CouponCodeType> codeTypes,
        @RequestParam(required = false) List<CouponChannel> channels,
        @RequestParam(required = false) String keyword,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime startFrom,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime endTo,

        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        CouponPageResDto res = couponService.getCouponList(
            statuses, codeTypes, channels, keyword, startFrom, endTo, pageable
        );

        return ResponseEntity.ok(ApiResponse.success("쿠폰 목록 조회 성공", res));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<CouponCreateResDto>> createCoupon(
        @Valid @RequestBody CouponCreateReqDto req) {

        CouponCreateResDto res = couponService.createCoupon(req);

        return ResponseEntity.ok(ApiResponse.success("쿠폰 생성 성공", res));

    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{coupon_id}")
    public ResponseEntity<ApiResponse<CouponUpdateResDto>> updateCoupon(
        @Valid @RequestBody CouponUpdateReqDto req,
        @PathVariable(name = "coupon_id") Long couponId) {

        CouponUpdateResDto res = couponService.updateCoupon(req, couponId);

        return ResponseEntity.ok(ApiResponse.success("쿠폰 수정 성공", res));

    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{coupon_id}")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(
        @PathVariable(name = "coupon_id") Long couponId) {

        couponService.deleteCoupon(couponId);

        return ResponseEntity.ok(ApiResponse.successNoData("쿠폰 삭제 성공"));

    }


}
