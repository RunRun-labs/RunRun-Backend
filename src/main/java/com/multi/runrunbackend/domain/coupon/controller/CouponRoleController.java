package com.multi.runrunbackend.domain.coupon.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponRoleCreateReqDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponRoleCreateResDto;
import com.multi.runrunbackend.domain.coupon.service.CouponRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
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


}
