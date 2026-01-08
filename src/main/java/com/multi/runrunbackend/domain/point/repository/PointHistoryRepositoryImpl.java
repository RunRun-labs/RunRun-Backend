package com.multi.runrunbackend.domain.point.repository;


import com.multi.runrunbackend.domain.point.dto.req.CursorPage;
import com.multi.runrunbackend.domain.point.dto.req.PointHistoryListReqDto;
import com.multi.runrunbackend.domain.point.dto.res.PointHistoryListResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : BoKyung
 * @description : 포인트 내역 Repository 구현체
 * @filename : PointHistoryRepositoryImpl
 * @since : 2026. 01. 05. 월요일
 */
@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public CursorPage<PointHistoryListResDto> searchPointHistory(
            PointHistoryListReqDto req,
            Long userId
    ) {
        int size = (req.getSize() == null || req.getSize() <= 0) ? 10 : Math.min(req.getSize(), 50);

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("limit", size + 1);

        // WHERE 조건
        StringBuilder where = new StringBuilder(" WHERE ph.user_id = :userId ");

        // 필터링: ALL / EARN / USE
        if (req.getFilter() != null && !"ALL".equals(req.getFilter())) {
            where.append(" AND ph.point_type = :pointType ");
            params.put("pointType", req.getFilter());
        }

        // 커서 처리
        if (req.getCursor() != null) {
            where.append(" AND ph.id < :cursor ");
            params.put("cursor", req.getCursor());
        }

        // SQL 구성
        StringBuilder sql = new StringBuilder();
        sql.append("""
                SELECT 
                    ph.id,
                    ph.reason,
                    ph.point_type,
                    ph.change_amount,
                    ph.created_at,
                    pp.product_name
                FROM point_history ph
                LEFT JOIN point_product pp ON ph.point_product_id = pp.id
                """);
        sql.append(where);
        sql.append(" ORDER BY ph.id DESC ");
        sql.append(" LIMIT :limit ");

        // 쿼리 실행
        List<PointHistoryListResDto> rows = jdbc.query(
                sql.toString(),
                params,
                (rs, rowNum) -> mapRow(rs)
        );

        return buildCursorPage(rows, size);
    }

    private PointHistoryListResDto mapRow(ResultSet rs) {
        try {
            LocalDateTime createdAt = null;
            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) {
                createdAt = ts.toLocalDateTime();
            }

            return PointHistoryListResDto.builder()
                    .id(rs.getLong("id"))
                    .reason(rs.getString("reason"))
                    .pointType(rs.getString("point_type"))
                    .amount(rs.getInt("change_amount"))
                    .transactionDate(createdAt)
                    .productName(rs.getString("product_name"))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to map row", e);
        }
    }

    private CursorPage<PointHistoryListResDto> buildCursorPage(
            List<PointHistoryListResDto> rows,
            int size
    ) {
        boolean hasNext = rows.size() > size;
        List<PointHistoryListResDto> items = hasNext ? rows.subList(0, size) : rows;

        Long nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            PointHistoryListResDto last = items.get(items.size() - 1);
            nextCursor = last.getId();
        }

        return CursorPage.<PointHistoryListResDto>builder()
                .items(items)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }
}
