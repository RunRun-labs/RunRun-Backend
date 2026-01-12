package com.multi.runrunbackend.domain.advertisement.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import com.multi.runrunbackend.domain.advertisement.dto.req.adplacement.AdPlacementAdminCreateReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.req.adplacement.AdPlacementAdminUpdateReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.adplacement.AdPlacementAdminListItemResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.common.PageResDto;
import com.multi.runrunbackend.domain.advertisement.service.AdPlacementAdminService;
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
 * @filename : AdAdminPlacementController
 * @since : 2026. 1. 11. Sunday
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/ad-placements")
public class AdAdminPlacementController {

    private final AdPlacementAdminService adPlacementAdminService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(
        @Valid @RequestBody AdPlacementAdminCreateReqDto dto) {
        return ResponseEntity.ok(
            ApiResponse.success("배치 생성 성공", adPlacementAdminService.
                create(dto))
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResDto<AdPlacementAdminListItemResDto>>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "5") int size,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) AdSlotType slotType,
        @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(
                "플레이스먼트 목록 조회 성공",
                adPlacementAdminService.listPlacements(page, size, isActive, slotType, keyword)
            )
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{placementId}")
    public ResponseEntity<ApiResponse<Void>> update(
        @PathVariable Long placementId,
        @Valid @RequestBody AdPlacementAdminUpdateReqDto dto
    ) {
        adPlacementAdminService.update(placementId, dto);
        return ResponseEntity.ok(ApiResponse.successNoData("배치 수정 성공"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{placementId}/disable")
    public ResponseEntity<ApiResponse<Void>> disable(@PathVariable Long placementId) {
        adPlacementAdminService.disable(placementId);
        return ResponseEntity.ok(ApiResponse.successNoData("배치 비활성화 성공"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{placementId}/enable")
    public ResponseEntity<ApiResponse<Void>> enable(@PathVariable Long placementId) {
        adPlacementAdminService.enable(placementId);
        return ResponseEntity.ok(ApiResponse.successNoData("배치 활성화 성공"));
    }
}
