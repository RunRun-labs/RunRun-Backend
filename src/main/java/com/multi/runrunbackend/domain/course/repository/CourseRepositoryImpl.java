package com.multi.runrunbackend.domain.course.repository;

import com.multi.runrunbackend.domain.course.constant.CourseRegisterType;
import com.multi.runrunbackend.domain.course.constant.CourseSortType;
import com.multi.runrunbackend.domain.course.dto.req.CourseListReqDto;
import com.multi.runrunbackend.domain.course.dto.req.CursorPage;
import com.multi.runrunbackend.domain.course.dto.res.CourseListResDto;
import com.multi.runrunbackend.domain.course.util.CourseCursorCodec;
import com.multi.runrunbackend.domain.course.util.CourseCursorCodec.CursorPayload;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseRepositoryImpl
 * @since : 2025. 12. 19. Friday
 */
@Repository
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbc;
    private final CourseCursorCodec cursorCodec;

    @Override
    public CursorPage<CourseListResDto> searchCourses(CourseListReqDto req) {

        int size = (req.getSize() == null || req.getSize() <= 0) ? 10 : Math.min(req.getSize(), 50);
        CourseSortType sortType =
            (req.getSortType() == null) ? CourseSortType.LATEST : req.getSortType();

        CursorPayload cursor = cursorCodec.decodeOrNull(req.getCursor());
        if (cursor != null && cursor.getSortType() != sortType) {
            cursor = null; // 정렬 바뀌면 커서 무시
        }

        boolean nearby = Boolean.TRUE.equals(req.getNearby());
        boolean needGeo = nearby || sortType == CourseSortType.DISTANCE;

        Map<String, Object> params = new HashMap<>();
        params.put("limit", size + 1);

        // 공통 WHERE
        StringBuilder where = new StringBuilder(" WHERE c.status = 'ACTIVE' ");

        // keyword
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            where.append(" AND (c.title ILIKE :kw OR c.description ILIKE :kw) ");
            params.put("kw", "%" + req.getKeyword().trim() + "%");
        }

        // register type
        if (req.getRegisterType() != null) {
            where.append(" AND c.register_type = :registerType ");
            params.put("registerType", req.getRegisterType().name());
        }

        // distance bucket filter (course.distance_m)
        Integer[] minMax = parseDistanceBucket(req.getDistanceBucket());
        if (minMax != null) {
            Integer min = minMax[0];
            Integer max = minMax[1];
            if (min != null) {
                where.append(" AND c.distance_m >= :minDist ");
                params.put("minDist", min);
            }
            if (max != null) {
                where.append(" AND c.distance_m < :maxDist ");
                params.put("maxDist", max);
            }
        }

        // 지오 필요하면 lat/lng 파라미터 세팅 (nearby=true 또는 sortType=DISTANCE)
        String pointExpr = null;
        String centerExpr = null;
        if (needGeo) {
            if (req.getLat() == null || req.getLng() == null) {
                throw new IllegalArgumentException("nearby=true 또는 DISTANCE 정렬이면 lat/lng는 필수");
            }
            params.put("lat", req.getLat());
            params.put("lng", req.getLng());

            // start_lat/start_lng로 point 만들어서 geography로 캐스팅 (미터 단위)
            pointExpr = "ST_SetSRID(ST_MakePoint(c.start_lng, c.start_lat), 4326)::geography";
            centerExpr = "ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography";
        }

        // nearby는 "필터" => WHERE에 반경 조건만 추가 (정렬은 sortType이 담당)
        if (nearby) {
            int radiusM =
                (req.getRadiusM() == null || req.getRadiusM() <= 0) ? 1500 : req.getRadiusM();
            params.put("radiusM", radiusM);

            where.append(" AND ST_DWithin(")
                .append(pointExpr)
                .append(", ")
                .append(centerExpr)
                .append(", :radiusM) ");
        }

        // 정렬 분기: nearby=true여도 FAVORITE/LIKE/LATEST로 정상 정렬되게 바꿈
        if (sortType == CourseSortType.DISTANCE) {
            return runDistanceQuery(req, where.toString(), params, cursor, size, pointExpr,
                centerExpr);
        }
        if (sortType == CourseSortType.LIKE) {
            // nearby=true면 dist_m도 같이 내려주기(표시용). 정렬은 like_count DESC.
            return runCountDescQuery(where.toString(), params, cursor, size,
                "c.like_count", CourseSortType.LIKE, nearby, pointExpr, centerExpr);
        }
        if (sortType == CourseSortType.FAVORITE) {
            return runCountDescQuery(where.toString(), params, cursor, size,
                "c.favorite_count", CourseSortType.FAVORITE, nearby, pointExpr, centerExpr);
        }

        // default: latest
        return runLatestQuery(where.toString(), params, cursor, size, nearby, pointExpr,
            centerExpr);
    }

    private CursorPage<CourseListResDto> runLatestQuery(
        String whereSql,
        Map<String, Object> params,
        CursorPayload cursor,
        int size,
        boolean includeDist,
        String pointExpr,
        String centerExpr
    ) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("""
              c.id, c.title, c.description,
              c.distance_m, c.start_lat, c.start_lng,
              c.thumbnail_url, c.image_url,
              c.like_count, c.favorite_count,
              c.register_type,
              c.address,
              c.created_at
            """);

        if (includeDist) {
            sql.append(", ST_Distance(").append(pointExpr).append(", ").append(centerExpr)
                .append(") AS dist_m ");
        }

        sql.append(" FROM course c ");
        sql.append(whereSql);

        // 커서: created_at desc, id desc
        if (cursor != null && cursor.getCreatedAt() != null && cursor.getId() != null) {
            sql.append(
                " AND (c.created_at < :cursorCreatedAt OR (c.created_at = :cursorCreatedAt AND c.id < :cursorId)) ");
            params.put("cursorCreatedAt", Timestamp.from(cursor.getCreatedAt().toInstant()));
            params.put("cursorId", cursor.getId());
        }

        sql.append(" ORDER BY c.created_at DESC, c.id DESC ");
        sql.append(" LIMIT :limit ");

        List<CourseListResDto> rows = jdbc.query(sql.toString(), params,
            (rs, rowNum) -> {
                try {
                    return mapRow(rs, includeDist);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        return buildCursorPage(rows, size, (last) -> {
            CursorPayload p = new CursorPayload();
            p.setSortType(CourseSortType.LATEST);
            p.setCreatedAt(last.getCreatedAt());
            p.setId(last.getId());
            return cursorCodec.encode(p);
        });
    }

    private CursorPage<CourseListResDto> runCountDescQuery(
        String whereSql,
        Map<String, Object> params,
        CursorPayload cursor,
        int size,
        String countColumn,
        CourseSortType sortType,
        boolean includeDist,
        String pointExpr,
        String centerExpr
    ) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("""
              c.id, c.title, c.description,
              c.distance_m, c.start_lat, c.start_lng,
              c.thumbnail_url, c.image_url,
              c.like_count, c.favorite_count,
              c.register_type,
              c.address,
              c.created_at
            """);

        if (includeDist) {
            sql.append(", ST_Distance(").append(pointExpr).append(", ").append(centerExpr)
                .append(") AS dist_m ");
        }

        sql.append(" FROM course c ");
        sql.append(whereSql);

        // 커서: count desc, id desc
        if (cursor != null && cursor.getCount() != null && cursor.getId() != null) {
            sql.append(" AND (")
                .append(countColumn).append(" < :cursorCount")
                .append(" OR (").append(countColumn)
                .append(" = :cursorCount AND c.id < :cursorId)) ");
            params.put("cursorCount", cursor.getCount());
            params.put("cursorId", cursor.getId());
        }

        sql.append(" ORDER BY ").append(countColumn).append(" DESC, c.id DESC ");
        sql.append(" LIMIT :limit ");

        List<CourseListResDto> rows = jdbc.query(sql.toString(), params,
            (rs, rowNum) -> {
                try {
                    return mapRow(rs, includeDist);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        return buildCursorPage(rows, size, (last) -> {
            CursorPayload p = new CursorPayload();
            p.setSortType(sortType);
            p.setCount(
                sortType == CourseSortType.LIKE ? last.getLikeCount() : last.getFavoriteCount());
            p.setId(last.getId());
            return cursorCodec.encode(p);
        });
    }

    // ✅ 여기만 고치면 되냐고 했던 그 부분 포함해서: start_point -> start_lat/start_lng로 계산
    private CursorPage<CourseListResDto> runDistanceQuery(
        CourseListReqDto req,
        String whereSql,
        Map<String, Object> params,
        CursorPayload cursor,
        int size,
        String pointExpr,
        String centerExpr
    ) {
        // dist_m 한번만 계산해서 ORDER/CURSOR에서 재사용
        StringBuilder sql = new StringBuilder();
        sql.append("WITH base AS ( ");
        sql.append("""
              SELECT
                c.id, c.title, c.description,
                c.distance_m, c.start_lat, c.start_lng,
                c.thumbnail_url, c.image_url,
                c.like_count, c.favorite_count,
                c.register_type,
                c.address,
                c.created_at,
            """);
        sql.append(" ST_Distance(").append(pointExpr).append(", ").append(centerExpr)
            .append(") AS dist_m ");
        sql.append(" FROM course c ");
        sql.append(whereSql);
        sql.append(" ) ");
        sql.append(" SELECT * FROM base WHERE 1=1 ");

        // 커서: dist asc, id asc
        if (cursor != null && cursor.getDistM() != null && cursor.getId() != null) {
            sql.append(" AND (dist_m > :cursorDist OR (dist_m = :cursorDist AND id > :cursorId)) ");
            params.put("cursorDist", cursor.getDistM());
            params.put("cursorId", cursor.getId());
        }

        sql.append(" ORDER BY dist_m ASC, id ASC ");
        sql.append(" LIMIT :limit ");

        List<CourseListResDto> rows = jdbc.query(sql.toString(), params,
            (rs, rowNum) -> {
                try {
                    return mapRow(rs, true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        return buildCursorPage(rows, size, (last) -> {
            CursorPayload p = new CursorPayload();
            p.setSortType(CourseSortType.DISTANCE);
            p.setDistM(last.getDistM());
            p.setId(last.getId());
            return cursorCodec.encode(p);
        });
    }

    private CourseListResDto mapRow(ResultSet rs, boolean includeDist) throws Exception {
        OffsetDateTime createdAt = null;
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            createdAt = ts.toInstant().atOffset(ZoneOffset.UTC);
        }

        CourseListResDto.CourseListResDtoBuilder b = CourseListResDto.builder()
            .id(rs.getLong("id"))
            .title(rs.getString("title"))
            .distanceM((Integer) rs.getObject("distance_m"))
            .address(rs.getString("address"))
            .thumbnailUrl(rs.getString("thumbnail_url"))
            .likeCount(((Number) rs.getObject("like_count")).longValue())
            .favoriteCount(((Number) rs.getObject("favorite_count")).longValue())
            .registerType(parseEnumSafe(rs.getString("register_type")))
            .createdAt(createdAt);

        if (includeDist) {
            Object d = rs.getObject("dist_m");
            b.distM(d == null ? null : ((Number) d).doubleValue());
        }

        return b.build();
    }

    private CourseRegisterType parseEnumSafe(String v) {
        if (v == null) {
            return null;
        }
        try {
            return CourseRegisterType.valueOf(v);
        } catch (Exception e) {
            return null;
        }
    }

    private CursorPage<CourseListResDto> buildCursorPage(
        List<CourseListResDto> rows,
        int size,
        java.util.function.Function<CourseListResDto, String> nextCursorFn
    ) {
        boolean hasNext = rows.size() > size;
        List<CourseListResDto> items = hasNext ? rows.subList(0, size) : rows;

        String nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            CourseListResDto last = items.get(items.size() - 1);
            nextCursor = nextCursorFn.apply(last);
        }

        return CursorPage.<CourseListResDto>builder()
            .items(items)
            .hasNext(hasNext)
            .nextCursor(nextCursor)
            .build();
    }

    private Integer[] parseDistanceBucket(String bucket) {
        if (bucket == null || bucket.isBlank()) {
            return null;
        }
        return switch (bucket.trim()) {
            case "0_3" -> new Integer[]{0, 3000};
            case "3_5" -> new Integer[]{3000, 5000};
            case "5_10" -> new Integer[]{5000, 10000};
            case "10_PLUS" -> new Integer[]{10000, null};
            default -> null;
        };
    }
}