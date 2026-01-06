package com.multi.runrunbackend.domain.coupon.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponRoleActiveReqDto;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponRoleCreateReqDto;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponRoleUpdateReqDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponRoleCreateResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponRoleListItemResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponRoleListReqDto;
import com.multi.runrunbackend.domain.coupon.service.CouponRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponRoleController
 * @since : 2025. 12. 29. Monday
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/coupon-roles")
public class CouponRoleController {

    private final CouponRoleService couponRoleService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<CouponRoleCreateResDto>> createCouponRole(
        @Valid @RequestBody CouponRoleCreateReqDto req
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("쿠폰 정책 생성 성공", couponRoleService.createCouponRole(req)));

    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{couponRole_id}")
    public ResponseEntity<ApiResponse<Void>> updateCouponRole(
        @Valid @RequestBody CouponRoleUpdateReqDto req,
        @PathVariable(name = "couponRole_id") Long couponRoleId
    ) {
        couponRoleService.updateCouponRole(req, couponRoleId);
        return ResponseEntity.ok(
            ApiResponse.successNoData("쿠폰 정책 수정 성공"));

    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{couponRole_id}")
    public ResponseEntity<ApiResponse<CouponRoleCreateResDto>> deleteCouponRole(
        @PathVariable(name = "couponRole_id") Long couponRoleId
    ) {
        couponRoleService.deleteCouponRole(couponRoleId);
        return ResponseEntity.ok(
            ApiResponse.successNoData("쿠폰 정책 삭제 성공"));

    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CouponRoleListItemResDto>>> getCouponRoleList(
        @Valid @ModelAttribute CouponRoleListReqDto req,
        @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("쿠폰 자동발급 정책 목록 조회 성공",
                couponRoleService.getCouponRoleList(req, pageable))
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{couponRoleId}")
    public ResponseEntity<ApiResponse<Void>> setActive(
        @PathVariable Long couponRoleId,
        @Valid @RequestBody CouponRoleActiveReqDto req
    ) {
        couponRoleService.setActive(couponRoleId, req.getIsActive());
        return ResponseEntity.ok(ApiResponse.successNoData("쿠폰 자동발급 정책 활성상태 변경 성공"));
    }


}
