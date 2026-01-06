package com.multi.runrunbackend.domain.point.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.point.dto.req.PointProductCreateReqDto;
import com.multi.runrunbackend.domain.point.dto.req.PointProductUpdateReqDto;
import com.multi.runrunbackend.domain.point.dto.res.PointProductCreateResDto;
import com.multi.runrunbackend.domain.point.dto.res.PointProductPageResDto;
import com.multi.runrunbackend.domain.point.dto.res.PointProductUpdateResDto;
import com.multi.runrunbackend.domain.point.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * @author : BoKyung
 * @description : 포인트 상품 관리자 컨트롤러
 * @filename : PointAdminController
 * @since : 2026. 01. 06. 화요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/points/products")
public class PointAdminController {

    private final PointService pointService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PointProductPageResDto>> listProducts(
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        PointProductPageResDto res = pointService.getProductList(
                isAvailable, keyword, pageable
        );
        return ResponseEntity.ok(ApiResponse.success("포인트 상품 목록 조회 성공", res));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<PointProductCreateResDto>> createProduct(
            @Valid @RequestBody PointProductCreateReqDto req
    ) {
        PointProductCreateResDto res = pointService.createProduct(req);
        return ResponseEntity.ok(ApiResponse.success("포인트 상품 생성 성공", res));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{product_id}")
    public ResponseEntity<ApiResponse<PointProductUpdateResDto>> updateProduct(
            @PathVariable(name = "product_id") Long productId,
            @Valid @RequestBody PointProductUpdateReqDto req
    ) {
        PointProductUpdateResDto res = pointService.updateProduct(productId, req);
        return ResponseEntity.ok(ApiResponse.success("포인트 상품 수정 성공", res));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{product_id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable(name = "product_id") Long productId
    ) {
        pointService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.successNoData("포인트 상품 삭제 성공"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file
    ) {
        Map<String, String> result = pointService.uploadProductImage(file);
        return ResponseEntity.ok(ApiResponse.success("이미지 업로드 성공", result));
    }
}
