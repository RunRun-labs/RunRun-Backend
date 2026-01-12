package com.multi.runrunbackend.domain.advertisement.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.advertisement.constant.AdSlotStatus;
import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import com.multi.runrunbackend.domain.advertisement.dto.req.adslot.AdSlotAdminCreateReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.req.adslot.AdSlotAdminUpdateReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.adslot.AdSlotAdminListItemResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.common.PageResDto;
import com.multi.runrunbackend.domain.advertisement.service.AdSlotAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
 * @filename : AdAdminSlotController
 * @since : 2026. 1. 9. Friday
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/ad-slot")
public class AdAdminSlotController {

    ;
    private final AdSlotAdminService adSlotAdminService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(
        @Valid @RequestBody AdSlotAdminCreateReqDto dto) {
        return ResponseEntity.ok(ApiResponse.success("슬롯 생성 성공", adSlotAdminService.create(dto)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResDto<AdSlotAdminListItemResDto>>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "5") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) AdSlotType slotType,
        @RequestParam(required = false) AdSlotStatus status
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("슬롯 목록 조회 성공",
                adSlotAdminService.list(page, size, keyword, slotType, status))
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{slotId}")
    public ResponseEntity<ApiResponse<Void>> update(
        @PathVariable Long slotId,
        @Valid @RequestBody AdSlotAdminUpdateReqDto dto
    ) {
        adSlotAdminService.update(slotId, dto);
        return ResponseEntity.ok(ApiResponse.successNoData("슬롯 수정 성공"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{slotId}/disable")
    public ResponseEntity<ApiResponse<Void>> disable(@PathVariable Long slotId) {
        adSlotAdminService.disable(slotId);
        return ResponseEntity.ok(ApiResponse.successNoData("슬롯 비활성화 성공"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{slotId}/enable")
    public ResponseEntity<ApiResponse<Void>> enable(@PathVariable Long slotId) {
        adSlotAdminService.enable(slotId);
        return ResponseEntity.ok(ApiResponse.successNoData("슬롯 활성화 성공"));
    }
}



