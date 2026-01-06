package com.multi.runrunbackend.domain.point.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @author : BoKyung
 * @description : 포인트 상품 목록 조회 결과와 페이징 정보를 담는 DTO
 * @filename : PointProductPageResDto
 * @since : 2026. 01. 06. 화요일
 */
@Getter
@AllArgsConstructor
public class PointProductPageResDto {

    private List<PointProductListItemResDto> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

}
