package com.multi.runrunbackend.domain.coupon.respository;

import com.multi.runrunbackend.domain.coupon.constant.CouponBenefitType;
import com.multi.runrunbackend.domain.coupon.constant.CouponChannel;
import com.multi.runrunbackend.domain.coupon.constant.CouponIssueSortType;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponIssueListItemResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponIssueListReqDto;
import com.multi.runrunbackend.domain.coupon.util.CouponIssueCursorCodec;
import com.multi.runrunbackend.domain.coupon.util.CouponIssueCursorCodec.CursorPayload;
import com.multi.runrunbackend.domain.course.dto.req.CursorPage;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponIssueRepositoryImpl
 * @since : 2025. 12. 29. Monday
 */
@Repository
@RequiredArgsConstructor
public class CouponIssueRepositoryImpl implements CouponIssueRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbc;
    private final CouponIssueCursorCodec cursorCodec;

    @AllArgsConstructor
    private static class Row {

        Long issueId;
        LocalDateTime sortStartAt;
        LocalDateTime sortEndAt;
        Integer sortBenefitValue;

        String code;
        String name;
        Integer benefitValue;
        LocalDateTime startAt;
        LocalDateTime endAt;
        CouponBenefitType benefitType;
        CouponChannel couponChannel;

        CouponIssueListItemResDto toDto() {
            return CouponIssueListItemResDto.builder()
                    .id(issueId)
                    .code(code)
                    .name(name)
                    .benefitValue(benefitValue)
                    .startAt(startAt)
                    .endAt(endAt)
                    .benefitType(benefitType)
                    .couponChannel(couponChannel)
                    .build();
        }
    }

    @Override
    public CursorPage<CouponIssueListItemResDto> searchIssuedCoupons(Long userId,
                                                                     CouponIssueListReqDto req) {

        int size = (req.getSize() == null || req.getSize() <= 0) ? 10 : Math.min(req.getSize(), 50);

        CouponIssueSortType sortType = (req.getSortType() == null)
                ? CouponIssueSortType.START_AT_ASC
                : req.getSortType();

        CursorPayload cursor = cursorCodec.decodeOrNull(req.getCursor());
        if (cursor != null && cursor.getSortType() != sortType) {
            cursor = null;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("limit", size + 1);

        StringBuilder where = new StringBuilder(" WHERE ci.user_id = :userId ");

        where.append(" AND ci.status = 'AVAILABLE' ");

        // ✅ 다중 필터: 같은 필드 여러개면 IN절 (OR 의미)
        if (req.getBenefitTypes() != null && !req.getBenefitTypes().isEmpty()) {
            where.append(" AND c.benefit_type IN (:benefitTypes) ");
            params.put("benefitTypes", req.getBenefitTypes().stream().map(Enum::name).toList());
        }

        if (req.getCouponChannels() != null && !req.getCouponChannels().isEmpty()) {
            // 🔥 기존 c.coupon_channel -> DB엔 없고 엔티티는 channel 이라서 c.channel
            where.append(" AND c.channel IN (:couponChannels) ");
            params.put("couponChannels", req.getCouponChannels().stream().map(Enum::name).toList());
        }

        if (req.getStartFrom() != null) {
            where.append(" AND c.start_at >= :startFrom ");
            params.put("startFrom", Timestamp.valueOf(req.getStartFrom()));
        }
        if (req.getStartTo() != null) {
            where.append(" AND c.start_at <= :startTo ");
            params.put("startTo", Timestamp.valueOf(req.getStartTo()));
        }
        if (req.getEndFrom() != null) {
            where.append(" AND c.end_at >= :endFrom ");
            params.put("endFrom", Timestamp.valueOf(req.getEndFrom()));
        }
        if (req.getEndTo() != null) {
            where.append(" AND c.end_at <= :endTo ");
            params.put("endTo", Timestamp.valueOf(req.getEndTo()));
        }

        StringBuilder sql = new StringBuilder();
        sql.append("""
                    SELECT
                        ci.id              AS issue_id,
                        c.code             AS coupon_code,
                        c.name             AS coupon_name,
                        c.benefit_value    AS benefit_value,
                        c.benefit_type     AS benefit_type,
                        c.start_at         AS start_at,
                        c.end_at           AS end_at,
                        c.channel          AS coupon_channel
                    FROM coupon_issue ci
                    JOIN coupon c ON c.id = ci.coupon_id
                """);
        sql.append(where);

        switch (sortType) {
            case START_AT_ASC -> {
                if (cursor != null && cursor.getStartAt() != null && cursor.getIssueId() != null) {
                    sql.append("""
                                AND (c.start_at > :cursorStartAt
                                     OR (c.start_at = :cursorStartAt AND ci.id > :cursorIssueId))
                            """);
                    params.put("cursorStartAt", instantToDbTimestamp(cursor.getStartAt()));
                    params.put("cursorIssueId", cursor.getIssueId());
                }
                sql.append(" ORDER BY c.start_at ASC, ci.id ASC ");
            }
            case END_AT_ASC -> {
                if (cursor != null && cursor.getEndAt() != null && cursor.getIssueId() != null) {
                    sql.append("""
                                AND (c.end_at > :cursorEndAt
                                     OR (c.end_at = :cursorEndAt AND ci.id > :cursorIssueId))
                            """);
                    params.put("cursorEndAt", instantToDbTimestamp(cursor.getEndAt()));
                    params.put("cursorIssueId", cursor.getIssueId());
                }
                sql.append(" ORDER BY c.end_at ASC, ci.id ASC ");
            }
            case BENEFIT_VALUE_ASC -> {
                if (cursor != null && cursor.getBenefitValue() != null
                        && cursor.getIssueId() != null) {
                    sql.append("""
                                AND (c.benefit_value > :cursorBenefitValue
                                     OR (c.benefit_value = :cursorBenefitValue AND ci.id > :cursorIssueId))
                            """);
                    params.put("cursorBenefitValue", cursor.getBenefitValue());
                    params.put("cursorIssueId", cursor.getIssueId());
                }
                sql.append(" ORDER BY c.benefit_value ASC, ci.id ASC ");
            }
        }

        sql.append(" LIMIT :limit ");

        List<Row> rows = jdbc.query(sql.toString(), params, (rs, rowNum) -> {
            try {
                return mapRow(rs);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        boolean hasNext = rows.size() > size;
        List<Row> pageRows = hasNext ? rows.subList(0, size) : rows;

        List<CouponIssueListItemResDto> items = pageRows.stream().map(Row::toDto).toList();

        String nextCursor = null;
        if (hasNext && !pageRows.isEmpty()) {
            Row last = pageRows.get(pageRows.size() - 1);

            CursorPayload next = new CursorPayload();
            next.setSortType(sortType);
            next.setIssueId(last.issueId);

            if (sortType == CouponIssueSortType.START_AT_ASC) {
                next.setStartAt(toInstant(last.sortStartAt));
            } else if (sortType == CouponIssueSortType.END_AT_ASC) {
                next.setEndAt(toInstant(last.sortEndAt));
            } else if (sortType == CouponIssueSortType.BENEFIT_VALUE_ASC) {
                next.setBenefitValue(last.sortBenefitValue);
            }

            nextCursor = cursorCodec.encode(next);
        }

        return CursorPage.<CouponIssueListItemResDto>builder()
                .items(items)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    private Row mapRow(ResultSet rs) throws Exception {
        Long issueId = rs.getLong("issue_id");

        LocalDateTime startAt = tsToLdt(rs.getTimestamp("start_at"));
        LocalDateTime endAt = tsToLdt(rs.getTimestamp("end_at"));

        Integer benefitValue = (Integer) rs.getObject("benefit_value");

        String bt = rs.getString("benefit_type");
        String ch = rs.getString("coupon_channel");

        CouponBenefitType benefitType = (bt == null) ? null : CouponBenefitType.valueOf(bt);
        CouponChannel couponChannel = (ch == null) ? null : CouponChannel.valueOf(ch);

        return new Row(
                issueId,
                startAt,
                endAt,
                benefitValue,
                rs.getString("coupon_code"),
                rs.getString("coupon_name"),
                benefitValue,
                startAt,
                endAt,
                benefitType,
                couponChannel
        );
    }

    private static LocalDateTime tsToLdt(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private static Instant toInstant(LocalDateTime ldt) {
        if (ldt == null) {
            return Instant.EPOCH;
        }
        return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }

    private static Timestamp instantToDbTimestamp(Instant instant) {
        if (instant == null) {
            return null;
        }
        LocalDateTime ldt = LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        return Timestamp.valueOf(ldt);
    }
}