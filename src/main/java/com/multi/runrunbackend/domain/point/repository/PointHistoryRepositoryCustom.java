package com.multi.runrunbackend.domain.point.repository;

import com.multi.runrunbackend.domain.point.dto.req.CursorPage;
import com.multi.runrunbackend.domain.point.dto.req.PointHistoryListReqDto;
import com.multi.runrunbackend.domain.point.dto.res.PointHistoryListResDto;

/**
 * @author : BoKyung
 * @description : 포인트 내역 Repository
 * @filename : PointHistoryRepositoryCustom
 * @since : 2026. 01. 05. 월요일
 */
public interface PointHistoryRepositoryCustom {
    CursorPage<PointHistoryListResDto> searchPointHistory(
            PointHistoryListReqDto req,
            Long userId
    );
}
