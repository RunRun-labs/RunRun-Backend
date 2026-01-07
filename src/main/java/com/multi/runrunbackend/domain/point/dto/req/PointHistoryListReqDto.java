package com.multi.runrunbackend.domain.point.dto.req;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author : BoKyung
 * @description : 포인트 내역 조회 요청 DTO
 * @filename : PointHistoryListReqDto
 * @since : 2026. 01. 05. 월요일
 */
@Getter
@Setter
@NoArgsConstructor
public class PointHistoryListReqDto {
    private Long cursor;       // 마지막 ID
    private Integer size;
    private String filter;     // ALL, EARN, USE
}
