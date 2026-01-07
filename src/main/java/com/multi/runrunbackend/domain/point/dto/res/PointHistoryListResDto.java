package com.multi.runrunbackend.domain.point.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * @author : BoKyung
 * @description : 포인트 내역 리스트 응답 DTO
 * @filename : PointHistoryListResDto
 * @since : 2026. 01. 05. 월요일
 */
@Getter
@Builder
public class PointHistoryListResDto {
    private Long id;
    private String reason;
    private String pointType;
    private Integer amount;
    private LocalDateTime transactionDate;
    private String productName;
}
