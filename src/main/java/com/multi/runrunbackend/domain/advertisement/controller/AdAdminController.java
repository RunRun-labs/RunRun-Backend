package com.multi.runrunbackend.domain.advertisement.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.advertisement.dto.req.ad.AdAdminCreateReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.req.ad.AdAdminUpdateReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.ad.AdAdminCreateResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.ad.AdAdminDetailResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.ad.AdAdminListItemResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.ad.AdAdminUpdateResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.adstats.AdStatsResDto;
import com.multi.runrunbackend.domain.advertisement.service.AdAdminService;
import com.multi.runrunbackend.domain.advertisement.service.AdStatsService;
import com.multi.runrunbackend.domain.advertisement.constant.StatsRange;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdController
 * @since : 2026. 1. 9. Friday
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/ads")
public class AdAdminController {

    private final AdAdminService adAdminService;
    private final AdStatsService adStatsService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<AdAdminCreateResDto>> create(
        @Valid @RequestPart("dto") AdAdminCreateReqDto dto,
        @RequestPart(value = "imageFile", required = true) MultipartFile imageFile
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("광고 생성 성공", adAdminService.create(dto, imageFile)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdAdminListItemResDto>>> list(
        @RequestParam(required = false) String keyword,
        @PageableDefault(size = 5) Pageable pageable
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("광고 목록 조회 성공", adAdminService.list(keyword, pageable)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{adId}")
    public ResponseEntity<ApiResponse<AdAdminDetailResDto>> detail(@PathVariable Long adId) {
        return ResponseEntity.ok(ApiResponse.success("광고 상세 조회 성공", adAdminService.detail(adId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{adId}/stats")
    public ResponseEntity<ApiResponse<AdStatsResDto>> getStats(
        @PathVariable Long adId,
        @RequestParam(required = false) StatsRange range
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("광고 통계 조회 성공", adStatsService.getAdStats(adId, range)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{adId}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<AdAdminUpdateResDto>> update(
        @PathVariable Long adId,
        @Valid @RequestPart("dto") AdAdminUpdateReqDto dto,
        @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("광고 수정 성공", adAdminService.update(adId, dto, imageFile)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{adId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long adId) {
        adAdminService.delete(adId);
        return ResponseEntity.ok(ApiResponse.successNoData("광고 삭제 성공"));
    }
}


