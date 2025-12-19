package com.multi.runrunbackend.domain.course.dto.req;

import com.multi.runrunbackend.domain.course.constant.CourseRegisterType;
import com.multi.runrunbackend.domain.course.constant.CourseSortType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseListReqDto
 * @since : 2025. 12. 19. Friday
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseListReqDto {

    // 검색
    private String keyword; // title/description ILIKE

    // 필터
    private CourseRegisterType registerType; // AI/AUTO/MANUAL
    private Boolean nearby; // 내 주변 코스(true면 lat/lng/radiusM 필요)
    private Double lat;
    private Double lng;
    private Integer radiusM; // 반경(m)

    // 거리 구간 필터 (distanceM 기준)
    // 예: "0_3", "3_5", "5_10", "10_PLUS"
    private String distanceBucket;

    // 정렬
    private CourseSortType sortType; // LATEST/LIKE/FAVORITE/DISTANCE

    // 커서/사이즈
    private String cursor;
    private Integer size;
}


